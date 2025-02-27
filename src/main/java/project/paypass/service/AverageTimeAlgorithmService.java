package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.BusTime;
import project.paypass.domain.GeofenceLocation;
import project.paypass.repository.BusTimeRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AverageTimeAlgorithmService {

    private final BusTimeRepository busTimeRepository;

    public AverageTimeAlgorithmService(BusTimeRepository busTimeRepository) {
        this.busTimeRepository = busTimeRepository;
    }

    public Map<String, List<Long>> algorithmStart(Map<String, List<Long>> busInfoMap, List<GeofenceLocation> geofenceLocations) {
        log.info("시간알고리즘 시작한 후 받아온 busInfoMap: {}", busInfoMap);

        // 변형된 routeId를 원래 routeId로 변환한 매핑 저장
        Map<String, String> originalRouteIdMap = new HashMap<>();
        Map<String, List<Long>> processedSequences = new HashMap<>();

        for (String modifiedRouteId : busInfoMap.keySet()) {
            String originalRouteId = modifiedRouteId.replaceAll("_\\d+$", ""); // _1, _2 제거
            originalRouteIdMap.put(modifiedRouteId, originalRouteId);
            processedSequences.put(originalRouteId, busInfoMap.get(modifiedRouteId));
        }

        log.info("변형된 routeId -> 원래 routeId 매핑: {}", originalRouteIdMap);
        log.info("원래 routeId 기준으로 정리된 sequence 리스트: {}", processedSequences);

        // 연속된 sequence 구간에 대해 판별 처리
        Map<String, List<Long>> boardedLocationsMap = new HashMap<>();

        for (String originalRouteId : processedSequences.keySet()) {
            List<Long> sequences = processedSequences.get(originalRouteId);
            log.info("판별할 사용 예상 경로의 원래 routeId: {}, sequences: {}", originalRouteId, sequences);

            // 원래 routeId 기준으로 BusTime 테이블 조회
            Map<Integer, Long> sequenceTimeMap = fetchExpectedTimes(originalRouteId);
            log.info("routeId별 버스 걸리는시간 데이터로 부터 가져온 예상 시간 {}: {}", originalRouteId, sequenceTimeMap);

            // 연속된 sequence 구간에서 이동 시간 비교
            List<GeofenceLocation> checkedStops = checkPossibleBoarding(geofenceLocations, sequenceTimeMap, sequences);

            if (!checkedStops.isEmpty()) {
                List<Long> checkedSequences = new ArrayList<>();
                for (GeofenceLocation stop : checkedStops) {
                    String busInfo = stop.getBusInfo();
                    for (Long seq : sequences) {
                        if (busInfo.contains(String.valueOf(seq)) && !checkedSequences.contains(seq)) {
                            checkedSequences.add(seq);
                        }
                    }
                }
                log.info("최종 판별된 탑승한 구간의 원래 routeId {}: {}", originalRouteId, checkedSequences);

                // 원래 routeId를 변형된 형태(_1, _2 포함)로 복원해서 저장
                for (String modifiedRouteId : originalRouteIdMap.keySet()) {
                    if (originalRouteIdMap.get(modifiedRouteId).equals(originalRouteId)) {
                        boardedLocationsMap.put(modifiedRouteId, new ArrayList<>(checkedSequences));
                    }
                }
            }
        }

        log.info("최종 리스트 (원래 routeId 복원된 상태) boardedLocationsMap : {}", boardedLocationsMap);
        return boardedLocationsMap; // 변형된 routeId 포함하여 리턴 <- 중복제거에 넘겨줄 맵
    }

    private Map<Integer, Long> fetchExpectedTimes(String routeId) {
        log.info("routeId별 예상시간 찾기: {}", routeId);
        List<BusTime> busTimes = busTimeRepository.findByRouteId(routeId);
        Map<Integer, Long> sequenceTimeMap = new HashMap<>();


        // BusTime 리스트에서 각각의 버스 시간 정보 처리
        for (int i = 0; i < busTimes.size() - 1; i++) {
            BusTime currentBus = busTimes.get(i);
            BusTime nextBus = busTimes.get(i + 1);

            try {
                // BOM 제거
                String currentDepartureTimeStr = String.valueOf(currentBus.getDepartureTime()).replace("\uFEFF", "");
                String nextArrivalTimeStr = String.valueOf(nextBus.getArrivalTime()).replace("\uFEFF", "");

                // DateTimeFormatter로 파싱
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

                // 문자열을 LocalDateTime으로 변환
                LocalDateTime currentDeparture = LocalDateTime.parse(currentDepartureTimeStr, formatter);
                LocalDateTime nextArrival = LocalDateTime.parse(nextArrivalTimeStr, formatter);

                // 이동 시간 계산 (분 단위)
                long expectedTime = Duration.between(currentDeparture, nextArrival).toMinutes();

                sequenceTimeMap.put(currentBus.getSequence(), expectedTime);
                log.info("routeId {}, sequence {} -> sequence {}, 예상시간: {} 분",
                        currentBus.getRouteId(), currentBus.getSequence(), nextBus.getSequence(), expectedTime);
            } catch (Exception e) {
                log.error("시간 변환 오류! routeId {}, sequence {}, departureTime {}, arrivalTime {}",
                        currentBus.getRouteId(), currentBus.getSequence(), currentBus.getDepartureTime(), nextBus.getArrivalTime(), e);
            }
        }
        return sequenceTimeMap;
    }

    private List<GeofenceLocation> checkPossibleBoarding(List<GeofenceLocation> geofenceLocations,
                                                         Map<Integer, Long> sequenceTimeMap,
                                                         List<Long> sequences) {
        log.info("탑승 가능성 있는 sequences: {}", sequences);
        List<GeofenceLocation> checkedStops = new ArrayList<>();

        for (int i = 0; i < sequences.size() - 1; i++) {
            long startSeq = sequences.get(i);
            long endSeq = sequences.get(i + 1);
            log.info("탑승 가능성 있는 sequence의 pair: {} -> {}", startSeq, endSeq);

            // GeofenceLocation에서 sequence가 startSeq와 endSeq인 데이터 찾기
            GeofenceLocation startStop = geofenceLocations.stream()
                    .filter(g -> g.getBusInfo().contains(String.valueOf(startSeq)))
                    .findFirst().orElse(null);
            GeofenceLocation endStop = geofenceLocations.stream()
                    .filter(g -> g.getBusInfo().contains(String.valueOf(endSeq)))
                    .findFirst().orElse(null);

            // fence_out_time을 사용하여 실제 이동 시간 계산
            long actualTime = Duration.between(startStop.getFenceOutTime(), endStop.getFenceInTime()).toMinutes();
            Long expectedTime = sequenceTimeMap.get((int) startSeq);

            log.info("실제 걸린 이동시간 sequence pair {} -> {}: {} 분, 데이터에 따른 예상시간은 : {} 분",
                    startSeq, endSeq, actualTime, expectedTime);

            // 오차범위(2분 이하 <- 변경가능) 내에 있으면 버스를 탔다고 판별
            if (expectedTime != null && Math.abs(actualTime - expectedTime) <= 20) {
                log.info("찾은 탑승 결과 {} -> {}", startSeq, endSeq);
                checkedStops.add(startStop);
                checkedStops.add(endStop);  // 끝나는 지점도 추가
            }
        }
        log.info("checkedStops: {}", checkedStops);

        return checkedStops;
    }
}
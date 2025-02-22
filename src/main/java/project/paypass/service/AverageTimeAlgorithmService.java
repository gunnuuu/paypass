package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.BusTime;
import project.paypass.domain.GeofenceLocation;
import project.paypass.repository.BusTimeRepository;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
public class AverageTimeAlgorithmService {

    private final BusTimeRepository busTimeRepository;

    public AverageTimeAlgorithmService(BusTimeRepository busTimeRepository) {
        this.busTimeRepository = busTimeRepository;
    }

    public List<GeofenceLocation> algorithmStart(Map<String, List<Long>> busInfoMap, List<GeofenceLocation> geofenceLocations) {
        log.info("시간알고리즘 시작한 후 받아온 busInfoMap: {}", busInfoMap);

        // 연속된 sequence 구간만 추출
        Map<String, List<Long>> consecutiveSequences = extractConsecutiveSequences(busInfoMap);
        log.info("추출된 연속된 sequence들을 가진 쌍들: {}", consecutiveSequences);

        // 연속된 sequence 구간에 대해 판별 처리
        List<GeofenceLocation> boardedLocations = new ArrayList<>();

        for (String routeId : consecutiveSequences.keySet()) {
            List<Long> sequences = consecutiveSequences.get(routeId);
            log.info("판별할 사용 예상 경로의 routeId: {}, sequences: {}", routeId, sequences);

            // routeId별 평균 이동 시간 맵 가져오기 (BusTime 테이블에서 조회)
            Map<Integer, Long> sequenceTimeMap = fetchExpectedTimes(routeId);
            log.info("routeId별 버스 걸리는시간 데이터로 부터 가져온 예상 시간 {}: {}", routeId, sequenceTimeMap);

            // 연속된 sequence 구간에서 이동 시간 비교
            List<GeofenceLocation> checkedStops = checkPossibleBoarding(geofenceLocations, sequenceTimeMap, sequences);

            if (!checkedStops.isEmpty()) {
                log.info("최종 판별된 탑승한 구간의 routeId {}: {}", routeId, checkedStops);
                boardedLocations.addAll(checkedStops); // 탑승한 위치들 추가
            }
        }

        log.info("최종 리스트: {}", boardedLocations);
        return boardedLocations; // 버스를 탑승한 위치들만 포함된 리스트 리턴
    }

    // 연속된 sequence 구간을 추출하는 메서드
    private Map<String, List<Long>> extractConsecutiveSequences(Map<String, List<Long>> busInfoMap) {
        log.info("busInfoMap으로 부터 연속된 구간 추출중...");

        Map<String, List<Long>> consecutiveSequences = new HashMap<>(); // 연속된 부분들 저장한 맵

        for (Map.Entry<String, List<Long>> entry : busInfoMap.entrySet()) {
            String routeId = entry.getKey();
            List<Long> sequences = entry.getValue();

            List<Long> consecutive = new ArrayList<>();
            Long prevSequence = null;

            for (Long sequence : sequences) {
                // 연속된 sequence일 경우
                if (prevSequence == null || sequence == prevSequence + 1) {
                    consecutive.add(sequence);
                } else {
                    // 연속이 끊어진 경우 (새로운 연속 구간 시작)
                    if (!consecutive.isEmpty()) {
                        consecutiveSequences.put(routeId, new ArrayList<>(consecutive));
                        log.info("연속된 sequence을 만족하는 구간별 routeId {}: {}", routeId, consecutive);
                    }
                    consecutive.clear();
                    consecutive.add(sequence);
                }
                prevSequence = sequence;
            }

            // 마지막 구간 추가 (연속이 끊어지지 않은 경우)
            if (!consecutive.isEmpty()) {
                consecutiveSequences.put(routeId, consecutive);
                log.info("연속된 sequence을 만족하는 구간별 routeId {}: {}", routeId, consecutive);
            }
        }

        return consecutiveSequences;
    }

    private Map<Integer, Long> fetchExpectedTimes(String routeId) {
        log.info("routeId별 예상시간 찾기: {}", routeId);
        List<BusTime> busTimes = busTimeRepository.findByRouteId(routeId);
        Map<Integer, Long> sequenceTimeMap = new HashMap<>();

        for (BusTime time : busTimes) {
            // 도착일시와 출발일시를 LocalTime으로 변환
            LocalTime arrival = LocalTime.parse(time.getArrivalTime().substring(8, 12)); // 시간:분으로 변환
            LocalTime departure = LocalTime.parse(time.getDepartureTime().substring(8, 12)); // 시간:분으로 변환

            // 이동 시간 계산 (분 단위)
            long expectedTime = Duration.between(departure, arrival).toMinutes();

            sequenceTimeMap.put(time.getSequence(), expectedTime);
            log.info("routeId {}, sequence {}, 예상시간: {} 분", routeId, time.getSequence(), expectedTime);
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
            if (expectedTime != null && Math.abs(actualTime - expectedTime) <= 2) {
                log.info("찾은 탑승 결과 {} -> {}", startSeq, endSeq);
                checkedStops.add(startStop);
                checkedStops.add(endStop);  // 끝나는 지점도 추가
            }
        }

        return checkedStops;
    }
}

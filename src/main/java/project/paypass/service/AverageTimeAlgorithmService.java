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

        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = sortByUserFenceInTime(geofenceLocations);

        // 최종 결과 저장할 맵 (변형된 routeId 유지)
        Map<String, List<Long>> boardedLocationsMap = new HashMap<>();

        // routeId : List<> geofenceLocation 형식의 Map 작성
        // route별 geofenceLocation Map 생성
        Map<String, List<GeofenceLocation>> geofenceLocationMap = makeGeofenceLocationMap(sortedGeofenceLocations);
        log.info("geofenceLocationMap = " + geofenceLocationMap);

        // routeId: Map<> fenceInTime, fenceOutTime 형식의 Map 작성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = makeTimeMap(geofenceLocationMap);
        log.info("timeMap = " + timeMap);

        for (String modifiedRouteId : busInfoMap.keySet()) {
            // 조회할 때만 _1, _2 제거
            String originalRouteId = modifiedRouteId.replaceAll("_\\d+$", "");
            List<Long> sequences = busInfoMap.get(modifiedRouteId);

            log.info("판별할 사용 예상 경로의 원래 routeId: {}, sequences: {}", originalRouteId, sequences);

            // 원래 routeI와 fenceIntime 기준으로 BusTime 테이블 조회
            Map<Integer, Long> sequenceTimeMap = fetchExpectedTimes(originalRouteId, sequences);
            log.info("routeId별 버스 걸리는시간 데이터로 부터 가져온 예상 시간 {}: {}", originalRouteId, sequenceTimeMap);

            // 연속된 sequence 구간에서 이동 시간 비교
            List<GeofenceLocation> checkedStops = checkPossibleBoarding(originalRouteId, geofenceLocations, sequenceTimeMap, timeMap, sequences);

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

                // 🛠 변형된 modifiedRouteId 그대로 저장
                boardedLocationsMap.put(modifiedRouteId, new ArrayList<>(checkedSequences));
            }
        }

        log.info("최종 리스트 (변형된 routeId 유지) boardedLocationsMap : {}", boardedLocationsMap);
        return boardedLocationsMap;
    }

    private Map<String, List<GeofenceLocation>> makeGeofenceLocationMap(List<GeofenceLocation> geofenceLocations) {
        Map<String, List<GeofenceLocation>> geofenceLocationMap = new TreeMap<>();

        List<String> busInfoList = makeBusInfoList(geofenceLocations);

        // busInfoList에서 routeId만을 추출해서 set 생성
        Set<String> routeIdSet = makeRouteIdSet(busInfoList);

        // geofenceLocationMap에서 key값 추가
        geofenceLocationMapPlusKey(geofenceLocationMap, routeIdSet);

        // geofenceLocationMap에서 value값 추가
        geofenceLocationMapPlusValue(geofenceLocationMap, geofenceLocations);

        return geofenceLocationMap;
    }

    private Map<String, List<Map<String, LocalDateTime>>> makeTimeMap(Map<String, List<GeofenceLocation>> geofenceLocationMap) {

        // 해당 Map에서 userFenceInTime과 userFenceOutTime을 활용한 Map 생성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = transformGeofenceLocationToTime(geofenceLocationMap);

        return timeMap;
    }

    private Map<Integer, Long> fetchExpectedTimes(String routeId, List<Long> sequences) {
        log.info("routeId별 예상시간 찾기: {}", routeId);

        List<BusTime> busTimes = busTimeRepository.findByRouteId(routeId);
        Map<Integer, Long> sequenceTimeMap = new HashMap<>();

        log.info("fetchExpectedTimes()에서 originalRouteId 조회: {}", routeId);

        // 예외 처리: busTimes 또는 sequences 데이터 부족 시 종료
        if (busTimes == null || busTimes.size() < 2) {
            log.warn("routeId {}에 대한 유효한 버스 시간 데이터가 부족합니다.", routeId);
            return sequenceTimeMap;
        }
        if (sequences == null || sequences.size() < 2) {
            log.warn("routeId {}의 sequence 리스트가 부족합니다.", routeId);
            return sequenceTimeMap;
        }

        for (int i = 0; i < sequences.size() - 1; i++) {
            Long currentSeq = sequences.get(i);
            Long nextSeq = sequences.get(i + 1);

            BusTime currentBus = busTimes.stream()
                    .filter(bus -> bus.getSequence() == currentSeq)
                    .findFirst()
                    .orElse(null);

            BusTime nextBus = busTimes.stream()
                    .filter(bus -> bus.getSequence() == nextSeq)
                    .findFirst()
                    .orElse(null);

            // 현재 또는 다음 버스 정보가 없으면 건너뛰기
            if (currentBus == null || nextBus == null) {
                log.warn("routeId {}, sequence {} 또는 sequence {}에 해당하는 버스 정보 없음. 건너뜀.", routeId, currentSeq, nextSeq);
                continue;
            }

            try {
                String currentArrivalTimeStr = String.valueOf(currentBus.getArrivalTime()).replace("\uFEFF", "");
                String nextArrivalTimeStr = String.valueOf(nextBus.getArrivalTime()).replace("\uFEFF", "");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                LocalDateTime currentDeparture = LocalDateTime.parse(currentArrivalTimeStr, formatter);
                LocalDateTime nextArrival = LocalDateTime.parse(nextArrivalTimeStr, formatter);

                // 역행하는 시간 또는 같은 시간 필터링
                if (!nextArrival.isAfter(currentDeparture)) {
                    log.warn("routeId {}, sequence {} -> sequence {} : 다음 도착 시간이 현재 출발 시간보다 이전이거나 동일함 ({} >= {}). 건너뜀.",
                            routeId, currentBus.getSequence(), nextBus.getSequence(), currentDeparture, nextArrival);
                    continue;
                }

                // 예상 이동 시간(분 단위)
                long expectedTime = Duration.between(currentDeparture, nextArrival).toMinutes();

                sequenceTimeMap.put(currentBus.getSequence(), expectedTime);
                log.info("routeId {}, sequence {} -> sequence {}, 예상시간: {} 분",
                        routeId, currentBus.getSequence(), nextBus.getSequence(), expectedTime);
            } catch (Exception e) {
                log.error("시간 변환 오류! routeId {}, sequence {}, arrivalTime {}, nextArrivalTime {}",
                        routeId, currentBus.getSequence(), currentBus.getArrivalTime(), nextBus.getArrivalTime(), e);
            }
        }
        return sequenceTimeMap;
    }

    private List<GeofenceLocation> checkPossibleBoarding(String routeId, List<GeofenceLocation> geofenceLocations,
                                                         Map<Integer, Long> sequenceTimeMap, Map<String, List<Map<String, LocalDateTime>>> timeMap,
                                                         List<Long> sequences) {
        log.info("탑승 가능성 있는 sequences: {}", sequences);
        List<GeofenceLocation> checkedStops = new ArrayList<>();

        // routeId에 해당하는 fenceInTime, fenceOutTime 정보 가져오기
        List<Map<String, LocalDateTime>> timeList = timeMap.get(routeId);

        if (timeList == null || timeList.size() < sequences.size()) {
            log.warn("routeId {}에 대한 timeList 데이터가 부족함", routeId);
            return checkedStops;
        }

        for (int i = 0; i < sequences.size() - 1; i++) {
            long startSeq = sequences.get(i);
            long endSeq = sequences.get(i + 1);
            log.info("탑승 가능성 있는 sequence의 pair: {} -> {}", startSeq, endSeq);

            // timeList에서 startSeq과 endSeq에 해당하는 fenceOutTime과 fenceInTime 가져오기
            Map<String, LocalDateTime> startTimeMap = timeList.get(i);
            Map<String, LocalDateTime> endTimeMap = timeList.get(i + 1);

            if (startTimeMap == null || endTimeMap == null) {
                log.warn("sequence {} 또는 {}에 대한 시간 데이터가 없음", startSeq, endSeq);
                continue;
            }

            LocalDateTime fenceOutTime = startTimeMap.get("fenceOutTime");
            LocalDateTime fenceInTime = endTimeMap.get("fenceInTime");

            if (fenceOutTime == null || fenceInTime == null) {
                log.warn("sequence {} -> {}: fenceOutTime 또는 fenceInTime 값이 없음", startSeq, endSeq);
                continue;
            }

            // 실제 이동 시간 계산
            long actualTime = Duration.between(fenceOutTime, fenceInTime).toMinutes();
            Long expectedTime = sequenceTimeMap.get((int) startSeq);

            log.info("실제 걸린 이동시간 sequence pair {} -> {}: {} 분, 데이터에 따른 예상시간은 : {} 분",
                    startSeq, endSeq, actualTime, expectedTime);

            // 오차 범위(기본 2분 → 20분으로 변경) 내에 있으면 버스를 탔다고 판별
            if (expectedTime != null && Math.abs(actualTime - expectedTime) <= 20) {
                log.info("탑승 확인: sequence {} -> {}", startSeq, endSeq);

                // geofenceLocations에서 startSeq과 endSeq에 해당하는 GeofenceLocation 찾아서 추가
                GeofenceLocation startStop = geofenceLocations.stream()
                        .filter(g -> g.getBusInfo().contains(String.valueOf(startSeq)))
                        .findFirst().orElse(null);

                GeofenceLocation endStop = geofenceLocations.stream()
                        .filter(g -> g.getBusInfo().contains(String.valueOf(endSeq)))
                        .findFirst().orElse(null);

                if (startStop != null) checkedStops.add(startStop);
                if (endStop != null) checkedStops.add(endStop);
            }
        }

        log.info("최종 체크된 탑승 정류장: {}", checkedStops);
        return checkedStops;
    }


    private Map<String, List<Map<String, LocalDateTime>>> transformGeofenceLocationToTime(Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap) {
        TreeMap<String, List<Map<String, LocalDateTime>>> timeMap = new TreeMap<>();

        for (var routeIdAndGeofenceLocationList : continuousGeofenceLocationMap.entrySet()) {
            String routeId = routeIdAndGeofenceLocationList.getKey();
            List<GeofenceLocation> geofenceLocationList = routeIdAndGeofenceLocationList.getValue();

            ArrayList<Map<String, LocalDateTime>> timeList = new ArrayList<>();
            for (GeofenceLocation geofenceLocation : geofenceLocationList) {
                Map<String, LocalDateTime> detailTimeMap = new TreeMap<>();
                detailTimeMap.put("fenceInTime", geofenceLocation.getFenceInTime());
                detailTimeMap.put("fenceOutTime", geofenceLocation.getFenceOutTime());

                timeList.add(detailTimeMap);
            }

            timeMap.put(routeId, timeList);
        }

        return timeMap;
    }
    private List<String> makeBusInfoList(List<GeofenceLocation> sortedGeofenceLocations) {
        // busInfo만 존재하는 List 생성
        List<String> busInfoList = new ArrayList<>();

        for (GeofenceLocation geofenceLocation : sortedGeofenceLocations) {
            String busInfo = geofenceLocation.stationBusInfo();

            busInfoList.add(busInfo);
        }

        return busInfoList;
    }
    private Set<String> makeRouteIdSet(List<String> busInfoList) {
        // busInfoList에서 각 busInfo의 routeId만 추출하여 set에 추가
        Set<String> localSet = new HashSet<>();
        // busInfoList를 쪼개서 oneStationInfoList 생성
        for (String oneStationInfo : busInfoList) {
            List<String> oneStationInfoList = Arrays.asList(oneStationInfo.replaceAll("^\\{|\\}$", "").split("},\\{"));
            // oneStationInfoList를 쪼개서 routeIdAndSequence 생성
            for (String oneBusInfo : oneStationInfoList) {
                List<String> routeIdAndSequence = Arrays.asList(oneBusInfo.split(","));
                String routeId = routeIdAndSequence.get(0);
                localSet.add(routeId);
            }
        }
        return localSet;
    }

    private void geofenceLocationMapPlusKey(Map<String, List<GeofenceLocation>> geofenceLocationMap, Set<String> routeIdSet) {
        for (String routeId : routeIdSet) {
            geofenceLocationMap.put(routeId, new ArrayList<>());
        }
    }

    private void geofenceLocationMapPlusValue(Map<String, List<GeofenceLocation>> geofenceLocationMap, List<GeofenceLocation> geofenceLocations) {
        for (var routeIdAndGeofenceLocationList : geofenceLocationMap.entrySet()) {
            String routeId = routeIdAndGeofenceLocationList.getKey();
            List<GeofenceLocation> geofenceLocationList = routeIdAndGeofenceLocationList.getValue();

            String pattern = ".*" + routeId + ".*";

            for (GeofenceLocation geofenceLocation : geofenceLocations) {
                if (geofenceLocation.stationBusInfo().matches(pattern)) {
                    geofenceLocationList.add(geofenceLocation);
                }
            } // end for

        } // end for
    }
    private List<GeofenceLocation> sortByUserFenceInTime(List<GeofenceLocation> geofenceLocationList) {
        return geofenceLocationList.stream()
                .sorted(Comparator.comparing(GeofenceLocation::userFenceInTime))
                .toList();
    }
}
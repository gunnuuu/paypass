package project.paypass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.BusTime;
import project.paypass.domain.GeofenceLocation;
import project.paypass.repository.BusTimeRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AverageTimeAlgorithmService {

    private final BusTimeRepository busTimeRepository;

    public Map<String, List<Long>> algorithmStart(Map<String, List<Long>> busInfoMap, List<GeofenceLocation> geofenceLocations) {
        // 최종 결과 저장할 맵 (변형된 routeId 유지)
        Map<String, List<Long>> boardedLocationsMap = new TreeMap<>();

        log.info("시간알고리즘 시작한 후 받아온 busInfoMap: {}", busInfoMap);

        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = sortByUserFenceInTime(geofenceLocations);

        // routeId : List<> geofenceLocation 형식의 Map 작성
        // route별 geofenceLocation Map 생성
        Map<String, List<GeofenceLocation>> geofenceLocationMap = makeGeofenceLocationMap(sortedGeofenceLocations);
        log.info("geofenceLocationMap = " + geofenceLocationMap);

        // routeId: Map<> fenceInTime, fenceOutTime 형식의 Map 작성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = makeTimeMap(geofenceLocationMap, busInfoMap);
        log.info("timeMap = " + timeMap);

        for (String modifiedRouteId : busInfoMap.keySet()) {
            // 조회할 때만 _1, _2 제거
            String originalRouteId = modifiedRouteId.replaceAll("_\\d+$", "");
            List<Long> sequences = busInfoMap.get(modifiedRouteId);

            log.info("판별할 사용 예상 경로의 원래 routeId: {}, sequences: {}", originalRouteId, sequences);

            // 원래 routeI와 fenceIntime 기준으로 BusTime 테이블 조회
            Map<Integer, Long> sequenceTimeMap = fetchExpectedTimes(originalRouteId, sequences);
            log.info("routeId별 버스 걸리는시간 데이터로 부터 가져온 예상 시간 {}: {}", originalRouteId, sequenceTimeMap);

            // 최종 버스탑승이 판별된 routeId와 sequences 저장한 맵
            Map<String, List<Long>> result = checkPossibleBoarding(originalRouteId, geofenceLocations, sequenceTimeMap, timeMap, sequences);

            // `boardedLocationsMap`에 각 결과를 추가할 때 덮어쓰지 않도록 처리
            for (Map.Entry<String, List<Long>> entry : result.entrySet()) {
                String key = entry.getKey();  // 현재 routeId_숫자 형식의 key
                List<Long> existingSequences = boardedLocationsMap.get(key);

                if (existingSequences != null) {
                    // 현재 존재하는 key 목록에서 가장 큰 group 번호 찾기
                    int maxGroupNumber = 0;
                    for (String existingKey : boardedLocationsMap.keySet()) {
                        if (existingKey.startsWith(key.split("_")[0] + "_")) {
                            String[] parts = existingKey.split("_");
                            if (parts.length == 2) {
                                try {
                                    maxGroupNumber = Math.max(maxGroupNumber, Integer.parseInt(parts[1]));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }

                    // 다음 group 번호 할당
                    String newKey = key.split("_")[0] + "_" + (maxGroupNumber + 1);
                    boardedLocationsMap.put(newKey, entry.getValue());
                } else {
                    boardedLocationsMap.put(key, entry.getValue());
                }
            }
        }

        log.info("최종 리스트 (같은 routeId 경우 _숫자 추가) boardedLocationsMap : {}", boardedLocationsMap);
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

    private Map<String, List<Map<String, LocalDateTime>>> makeTimeMap(Map<String, List<GeofenceLocation>> geofenceLocationMap, Map<String, List<Long>> busInfoMap) {
        // 해당 Map에서 연속적인 sequence를 만족하는 데이터들만 남긴 consequenceMap 생성
        Map<String, List<GeofenceLocation>> consequenceMap = remainGeofenceLocationToConsequence(geofenceLocationMap, busInfoMap);


        // 해당 Map에서 userFenceInTime과 userFenceOutTime을 활용한 timeMap 생성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = transformGeofenceLocationToTime(consequenceMap);

        return timeMap;
    }


    private Map<Integer, Long> fetchExpectedTimes(String routeId, List<Long> sequences) {
        log.info("routeId별 예상시간 찾기: {}", routeId);

        List<BusTime> busTimes = busTimeRepository.findByRouteId(routeId);
        Map<Integer, Long> sequenceTimeMap = new TreeMap<>();

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
                String currentArrivalTimeStr = String.valueOf(currentBus.getArrivalTime()).replace("\uFEFF", "").trim();
                String nextArrivalTimeStr = String.valueOf(nextBus.getArrivalTime()).replace("\uFEFF", "").trim();

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

    private Map<String, List<Long>> checkPossibleBoarding(String routeId, List<GeofenceLocation> geofenceLocations,
                                                          Map<Integer, Long> sequenceTimeMap, Map<String, List<Map<String, LocalDateTime>>> timeMap,
                                                          List<Long> sequences) {
        log.info("탑승 가능성 있는 sequences: {}", sequences);
        Map<String, List<Long>> boardedLocationsMap = new TreeMap<>();

        // routeId에 해당하는 fenceInTime, fenceOutTime 정보 가져오기
        List<Map<String, LocalDateTime>> timeList = timeMap.get(routeId);

        if (timeList == null || timeList.size() < sequences.size()) {
            log.warn("routeId {}에 대한 timeList 데이터가 부족함", routeId);
            return boardedLocationsMap;
        }

        // 연속된 sequence들을 그룹화
        List<List<Long>> groupedSequences = groupConsecutiveSequences(sequences);

        int groupCounter = 1;  // 그룹 번호를 매기기 위한 카운터
        for (List<Long> group : groupedSequences) {
            String currentKey = routeId + "_" + groupCounter;  // _숫자 붙여서 구간 구별
            log.info("현재 그룹: {}", group);

            // sequences 중에서 groupSequences의 인덱스 구하기
            int index = Collections.indexOfSubList(sequences, group);
            System.out.println("sequences = " + sequences);
            System.out.println("groupedSequences = " + groupedSequences);
            System.out.println("group = " + group);

            System.out.println("index = " + index);


            // 구간 처리
            for (int i = 0; i < group.size() - 1; i++) {
                long startSeq = group.get(i);
                long endSeq = group.get(i + 1);
                log.info("탑승 가능성 있는 sequence의 pair: {} -> {}", startSeq, endSeq);

                // timeList에서 startSeq과 endSeq에 해당하는 fenceOutTime과 fenceInTime 가져오기
                Map<String, LocalDateTime> startTimeMap = timeList.get(index+ i);
                Map<String, LocalDateTime> endTimeMap = timeList.get(index + i + 1);

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

                // 오차 범위(기본 20분) 내에 있으면 버스를 탔다고 판별
                if (expectedTime != null && Math.abs(actualTime - expectedTime) <= 20) {
                    log.info("탑승 확인: sequence {} -> {}", startSeq, endSeq);

                    // 구간 번호와 연속된 sequence 값을 put
                    boardedLocationsMap.put(currentKey, new ArrayList<>(group));
                }
            }
            groupCounter++;
        }

        log.info("최종 체크된 탑승 정류장: {}", boardedLocationsMap);

        return boardedLocationsMap;
    }


    // 연속된 sequence를 그룹화하는 메서드
    private List<List<Long>> groupConsecutiveSequences(List<Long> sequences) {
        List<List<Long>> groupedSequences = new ArrayList<>();
        List<Long> currentGroup = new ArrayList<>();

        for (int i = 0; i < sequences.size(); i++) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(sequences.get(i));
            } else {
                // 연속되는 sequence만 그룹에 추가
                long prev = currentGroup.get(currentGroup.size() - 1);
                if (sequences.get(i) == prev + 1) {
                    currentGroup.add(sequences.get(i));
                } else {
                    groupedSequences.add(new ArrayList<>(currentGroup));
                    currentGroup.clear();
                    currentGroup.add(sequences.get(i));
                }
            }
        }
        // 마지막 그룹을 추가
        if (!currentGroup.isEmpty()) {
            groupedSequences.add(currentGroup);
        }

        return groupedSequences;
    }

    //연속적인 seqeunce쌍들만 남기기
    private Map<String, List<GeofenceLocation>> remainGeofenceLocationToConsequence(Map<String, List<GeofenceLocation>> geofenceLocationMap, Map<String, List<Long>> busInfoMap) {
        Map<String, List<GeofenceLocation>> consequenceMap = new TreeMap<>();

        Set<String> originRouteIdSet = busInfoMap.keySet().stream()
                .map(this::toOriginRouteId)
                .collect(Collectors.toSet());

        for (String routeId : geofenceLocationMap.keySet()) {
            List<GeofenceLocation> geofenceLocationList = geofenceLocationMap.get(routeId);

            // 인자가 하나이거나 연속된 수가 존재하지 않으면 스킵
            if (geofenceLocationList.size() < 2 || !originRouteIdSet.contains(routeId)) {
                continue;
            }

            // 연속된 sequence만 남기기
            List<GeofenceLocation> filteredLocations = filterConsecutiveSequences(geofenceLocationList, routeId);

            consequenceMap.put(routeId, filteredLocations);
        }

        log.info("consequenceMap 생성 완료: {}", consequenceMap);
        return consequenceMap;
    }


    private List<GeofenceLocation> filterConsecutiveSequences(List<GeofenceLocation> geofenceLocationList, String routeId) {
        List<GeofenceLocation> result = new ArrayList<>();
        List<GeofenceLocation> temp = new ArrayList<>();

        // busInfoMap처럼 String 형태로 sequence 값 추출
        List<Long> sequencesTemp = extractSequencesFromBusInfo(geofenceLocationList, routeId);
        System.out.println("sequencesTemp = " + sequencesTemp);
        List<Long> sequences = optimizeSequence(sequencesTemp);
        System.out.println("sequences = " + sequences);

        // sequence가 하나만 있거나 아예 없을 때를 대비한 예외 처리
        if (sequences.isEmpty()) {
            log.warn("sequences 리스트가 비어있습니다.");
            return result;
        }

        for (int i = 0; i < sequences.size(); i++) {
            // 처음에는 temp에 첫 번째 시퀀스를 넣는다
            if (temp.isEmpty()) {
                temp.add(geofenceLocationList.get(i));
            } else {
                // 현재 sequence가 이전 sequence와 1씩 증가하는지 확인
                if (sequences.get(i) == sequences.get(i - 1) + 1) {
                    temp.add(geofenceLocationList.get(i)); // 연속된 sequence일 경우 temp에 추가
                } else {
                    if (temp.size() > 1) { // 2개 이상이면 연속된 시퀀스로 저장
                        result.addAll(temp);
                    }
                    temp.clear(); // 새로운 구간 시작
                    temp.add(geofenceLocationList.get(i)); // 새로운 구간의 첫 번째 시퀀스를 추가
                }
            }
        }

        // 마지막 요소 추가 처리 (마지막 인덱스에 대한 처리)
        if (!temp.isEmpty() && temp.size() > 1) {
            result.addAll(temp);
        }

        return result;
    }


    private List<Long> extractSequencesFromBusInfo(List<GeofenceLocation> geofenceLocationList, String routeId) {
        List<Long> sequences = new ArrayList<>();

        for (GeofenceLocation geofenceLocation : geofenceLocationList) {
            String busInfo = geofenceLocation.stationBusInfo(); // busInfo 가져오기

            List<Long> temp = new ArrayList<>();
            // busInfo에서 해당 routeId에 맞는 sequence 찾기
            for (String oneBunInfo : busInfo.split("},\\{")) {

                oneBunInfo = oneBunInfo.replaceAll("[{}]", "");  // `{100100014,1}` -> `100100014,1`
                String[] parts = oneBunInfo.split(",");    // `100100014,1` -> `["100100014", "1"]`
                if (parts.length == 2 && parts[0].equals(routeId)) {
                    temp.add(Long.parseLong(parts[1]));
                }

            }
            if (temp.size() == 2) {
                long orNumber = Long.parseLong(temp.get(0) + "000" + temp.get(1));
                sequences.add(orNumber);
            }

            if (temp.size() == 1) {
                sequences.add(temp.get(0));
            }

            if (temp.size() != 1 && temp.size() != 2) {
                throw new RuntimeException("extractSequencesFromBusInfo 메서드에서 에러 발생");
            }

        }

        return sequences;
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

    private String toOriginRouteId(String routeId) {
        return Arrays.asList(routeId.split("_")).get(0);
    }


    // gpt 코드 (리펙토링 필요)
    public static List<Long> optimizeSequence(List<Long> sequences) {
        List<List<Long>> possibleCombinations = new ArrayList<>();

        // 각 숫자를 앞 2자리 & 뒷 2자리로 변환
        for (Long num : sequences) {
            possibleCombinations.add(getPossibleValues(num));
        }

        // 최적의 연속된 숫자 조합 선택
        return findBestSequence(possibleCombinations);
    }

    private static List<Long> getPossibleValues(Long num) {
        String strNum = String.valueOf(num);
        List<Long> values = new ArrayList<>();

        // 숫자 자릿수 확인
        int length = strNum.length();

        if (length >= 7) {
            // 길이가 7자리 이상일 경우: 앞 2자리 + 뒤 3자리
            values.add(Long.parseLong(strNum.substring(0, 2))); // 앞 2자리
            values.add(Long.parseLong(strNum.substring(length - 3))); // 뒤 3자리
        } else if (length >= 5) {
            // 길이가 5자리 이상 7자리 미만일 경우: 앞 2자리 + 뒤 2자리
            values.add(Long.parseLong(strNum.substring(0, 2))); // 앞 2자리
            values.add(Long.parseLong(strNum.substring(length - 2))); // 뒤 2자리
        } else {
            // 길이가 5자리 미만일 경우: 그대로 추가
            values.add(num);
        }

        return values;
    }

    private static List<Long> findBestSequence(List<List<Long>> optionsList) {
        List<Long> bestSequence = new ArrayList<>();
        Long prev = null;

        for (List<Long> options : optionsList) {
            if (options.size() == 1) {  // 리스트 크기가 1이면 그대로 추가
                bestSequence.add(options.get(0));
            } else {
                if (prev == null) {
                    bestSequence.add(options.get(1)); // 첫 번째 숫자는 뒷 2자리 선택
                } else {
                    // 이전 숫자와 가장 연속된 숫자 찾기
                    Long bestOption = options.get(1); // 기본적으로 뒷 2자리 선택
                    for (Long option : options) {
                        if (prev + 1 == option) { // 연속된 숫자가 있으면 선택
                            bestOption = option;
                            break;
                        }
                    }
                    bestSequence.add(bestOption);
                }
            }
            prev = bestSequence.get(bestSequence.size() - 1);
        }

        return bestSequence;
    }

}
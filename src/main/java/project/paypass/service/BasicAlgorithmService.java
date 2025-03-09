package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.Tree;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;

import java.util.*;

@Slf4j
@Service
public class BasicAlgorithmService {

    public Map<String, List<Long>> algorithmStart(List<GeofenceLocation> geofenceLocations) {
        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = sortByUserFenceInTime(geofenceLocations);

        Map<String, List<Long>> continuousBusInfoMap = basicLogic(sortedGeofenceLocations);

        return continuousBusInfoMap;
    }

    private Map<String, List<Long>> basicLogic(List<GeofenceLocation> sortedGeofenceLocations) {

        // busInfo만 존재하는 List 생성
        List<String> busInfoList = makeBusInfoList(sortedGeofenceLocations);
        log.info("busInfoList = " + busInfoList);

        // busInfoList를 통하여 busInfoMap 생성
        Map<String, List<Long>> busInfoMap = makeBusInfoMap(busInfoList);
        log.info("busInfoMap = " + busInfoMap);

        // 모든 경우에 수 중에서 가장 연속된 구간이 긴 busInfoMap만 살리기
        Map<String, List<Long>> maxContinuousMap = filterMaxContinuousMap(busInfoMap, busInfoList);
        log.info("maxContinuousMap = " + maxContinuousMap);

        // sequence가 순차적으로 증가하는지 검사
        // sequence의 일정 부분만 조건 만족 시 해당 부분의 sequence만 추출
        // 조건 만족 시 해당 sequence를 가지는 map 추가
        Map<String, List<Long>> continuousBusInfoMap = makeContinuousBusInfoMap(maxContinuousMap);
        log.info("continuousBusInfoMap = " + continuousBusInfoMap);

        return continuousBusInfoMap;
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

    private Map<String, List<Long>> makeBusInfoMap(List<String> busInfoList) {
        Map<String, List<Long>> busInfoMap = new TreeMap<>();

        // busInfoList에서 routeId만을 추출해서 set 생성
        Set<String> routeIdSet = makeRouteIdSet(busInfoList);

        // busInfoMap에서 key값 추가
        busInfoMapPlusKey(busInfoMap, routeIdSet);

        // busInfoMap에서 value값 추가
        busInfoMapPlusValue(busInfoMap, busInfoList);
        log.info("000이 제거되기 전 busInfoMap" + busInfoMap);

        // 중복되는 routeId를 해결하기 위해
        // sequence 값에 000이 있으면 경우의 수를 두개로 분리
        Map<String, List<Long>> divideBusInfoMap = divideBusInfoMap(busInfoMap);

        // 분리되지 않은 key 값들에게 _1 부여
        Map<String, List<Long>> finalMap = plueOneToOriginBusInfoMap(divideBusInfoMap);

        return finalMap;
    }

    private Map<String, List<Long>> filterMaxContinuousMap(Map<String, List<Long>> busInfoMap, List<String> busInfoList) {

        // 연속된 숫자의 개수를 나타내는 continuousCountMap 작성
        Map<String, Long> continuousCountMap = makeContinuousCountMap(busInfoMap);
        log.info("continuousCountMap = " + continuousCountMap);

        // 최대값을 나타내는 maxMap 작성
        Map<String, Long> maxMap = makeMaxMap(continuousCountMap, busInfoList);
        log.info("maxMap = " + maxMap);

        // 해당 maxMap을 만족하는 routeId를 가진 maxContinuousList 작성
        List<String> maxContinuousList = makeMaxContinuousList(continuousCountMap, maxMap);
        log.info("maxContinuousList = " + maxContinuousList);

        // 최대값을 가지는 List를 출력하는 maxContinuousMap 작성
        // Map에서 가장 적게 분할되는 routeId 별 List만 남기기
        Map<String, List<Long>> maxContinuousMap = makeMaxContinuousMap(busInfoMap, maxMap, maxContinuousList);

        return maxContinuousMap;
    }

    private Map<String, List<Long>> makeContinuousBusInfoMap(Map<String, List<Long>> busInfoMap) {
        Map<String, List<Long>> continuousBusInfoMap = new TreeMap<>();

        for (String routeId : busInfoMap.keySet()) {
            List<Long> sequenceList = busInfoMap.get(routeId);
            List<List<Long>> continuousSequenceList = checkSequential(sequenceList);

            for (List<Long> continuousSequences : continuousSequenceList) {
                // 여기서 Map으로 저장
                // Map Ket Name _? 으로 생성해야한다

                // route가 이미 존재하면 다음 _? 값을 배정해야한다
                // _가 이미 붙어있는 상황을 생각해야한다.
                String keyName = assignKeyName(continuousBusInfoMap, routeId);

                continuousBusInfoMap.put(keyName, continuousSequences);
            }
        }

        return continuousBusInfoMap;
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

    private void busInfoMapPlusKey(Map<String, List<Long>> busInfoMap, Set<String> routeIdSet) {
        for (String routeId : routeIdSet) {
            busInfoMap.put(routeId, new ArrayList<>());
        }
    }

    private void busInfoMapPlusValue(Map<String, List<Long>> busInfoMap, List<String> busInfoList) {
        // busInfoList를 쪼개서 oneStationInfoList 생성
        for (String oneStationInfo : busInfoList) {
            List<String> oneStationInfoList = Arrays.asList(oneStationInfo.replaceAll("^\\{|\\}$", "").split("},\\{"));

            // oneStationInfoList에 중복의 routeId가 있다면
            // sequence 값을 000으로 결합
            // [{100100317,8},{100100307,14},{100100316,24},{100100316,53},{100100308,69}]
            // -> [{100100317,8},{100100307,14},{100100316,2400053},{100100308,69}]
            List<String> checkedOneStationInfoList = checkDuplicateRouteId(oneStationInfoList);

            // oneStationInfoList를 쪼개서 routeIdAndSequence 생성
            for (String oneBusInfo : checkedOneStationInfoList) {
                List<String> routeIdAndSequence = Arrays.asList(oneBusInfo.split(","));
                String routeId = routeIdAndSequence.get(0);
                Long sequence = stringToLong(routeIdAndSequence.get(1));

                // sequence 값 추가하기
                List<Long> sequenceList = busInfoMap.get(routeId);
                sequenceList.add(sequence);
            }
        }
    }

    private Map<String, List<Long>> divideBusInfoMap(Map<String, List<Long>> busInfoMap) {
        // 수정하기 위한 복사본 생성
        Map<String, List<Long>> updatedBusInfoMap = new TreeMap<>(busInfoMap);
        // 삭제할 key를 저장할 리스트 생성
        List<String> keysToRemove = new ArrayList<>();

        for (var oneBusInfo : busInfoMap.entrySet()) {
            String routeId = oneBusInfo.getKey();
            List<Long> sequenceList = oneBusInfo.getValue();

            for (int i = 0; i < sequenceList.size(); i++) {
                Long sequence = sequenceList.get(i);
                String stringSequence = String.valueOf(sequence);

                if (stringSequence.contains("000")) {
                    List<String> twoSequence = Arrays.asList(stringSequence.split("000"));
                    Long firstSequence = Long.parseLong(twoSequence.get(0));
                    Long secondSequence = Long.parseLong(twoSequence.get(1));

                    // 해당 로직이 처음일 경우
                    if (!routeId.contains("_")) {
                        sequenceList.set(i, firstSequence);
                        updatedBusInfoMap.put(routeId + "_1", new ArrayList<>(sequenceList));
                        sequenceList.set(i, secondSequence);
                        updatedBusInfoMap.put(routeId + "_2", new ArrayList<>(sequenceList));

                        // 기존 routeId는 삭제해야 하므로 목록에 추가
                        keysToRemove.add(routeId);
                        break;
                    }

                    // 해당 로직이 다회차일 때
                    if (routeId.contains("_")) {
                        // ex) 123123123_1 -> [123123123,1]
                        List<String> routeIdAndCount = Arrays.asList(routeId.split("_"));
                        String pureRouteId = routeIdAndCount.get(0);
                        String currentCount = routeIdAndCount.get(1);

                        Long count = findMaxCount(updatedBusInfoMap, pureRouteId);

                        sequenceList.set(i, firstSequence);
                        updatedBusInfoMap.put(pureRouteId + "_" + currentCount, new ArrayList<>(sequenceList));

                        sequenceList.set(i, secondSequence);
                        updatedBusInfoMap.put(pureRouteId + "_" + (count + 1), new ArrayList<>(sequenceList));
                        break;
                    }
                }

            }
        } // for문 종료

        // 기존 맵에서 첫 분리된 key 삭제
        for (String key : keysToRemove) {
            updatedBusInfoMap.remove(key);
        }

        // 원본 맵을 최신 데이터로 갱신
        busInfoMap.clear();
        busInfoMap.putAll(updatedBusInfoMap);

        if (check000(busInfoMap)) {
            return divideBusInfoMap(busInfoMap);
        }

        return busInfoMap;
    }

    private Map<String, List<Long>> plueOneToOriginBusInfoMap(Map<String, List<Long>> divideBusInfoMap) {
        Map<String, List<Long>> finalMap = new TreeMap<>();

        for (var routeIdAndSequenceList : divideBusInfoMap.entrySet()) {
            String routeId = routeIdAndSequenceList.getKey();
            List<Long> sequenceList = routeIdAndSequenceList.getValue();

            if (!routeId.contains("_")){
                routeId += "_1";
            }

            finalMap.put(routeId, sequenceList);
        }

        return finalMap;
    }

    private Map<String, Long> makeContinuousCountMap(Map<String, List<Long>> busInfoMap){
        Map<String, Long> continuousCountMap = new TreeMap<>();

        for (var routeIdAndSequenceList : busInfoMap.entrySet()) {
            String routeId = routeIdAndSequenceList.getKey();
            List<Long> sequenceList = routeIdAndSequenceList.getValue();

            Long continuousCount = calculateContinuousCount(sequenceList);
            continuousCountMap.put(routeId, continuousCount);
        }

        return continuousCountMap;
    }

    private Map<String, Long> makeMaxMap(Map<String, Long> continuousCountMap, List<String> busInfoList) {
        Map<String, Long> maxMap = new TreeMap<>();

        List<String> routeIdList = makeRouteIdSet(busInfoList).stream().sorted().toList();

        for (String originalRouteId : routeIdList) {
            Long maxCount = 0L;
            for (var routeIdAndContinuousCount : continuousCountMap.entrySet()) {
                String routeId = routeIdAndContinuousCount.getKey();
                Long continuousCount = routeIdAndContinuousCount.getValue();

                String routeIdForCheck = Arrays.asList(routeId.split("_")).get(0);
                // originalRouteId와 비교하여 maxCount 갱신
                if (originalRouteId.equals(routeIdForCheck) && continuousCount > maxCount) {
                    maxCount = continuousCount;
                }
            } // for문 종료

            // maxMap에서 0 값인 routeId 삭제
            // -> 인자가 하나거나 연속되는 부분이 존재하지 않는 routeId 제거
            if (maxCount != 0) {
                maxMap.put(originalRouteId, maxCount);
            }

        } // for문 종료
        return maxMap;
    }

    private List<String> makeMaxContinuousList(Map<String, Long> continuousCountMap, Map<String, Long> maxMap) {
        List<String> maxContinuousList = new ArrayList<>();

        for (var originalRouteIdAndMaxCount : maxMap.entrySet()) {
            String originalRouteId = originalRouteIdAndMaxCount.getKey();
            Long maxCount = originalRouteIdAndMaxCount.getValue();

            for (var routeIdAndContinuousCount : continuousCountMap.entrySet()) {
                String routeId = routeIdAndContinuousCount.getKey();
                Long continuousCount = routeIdAndContinuousCount.getValue();

                String routeIdForCheck = Arrays.asList(routeId.split("_")).get(0);
                if (originalRouteId.equals(routeIdForCheck) && maxCount == continuousCount){
                    maxContinuousList.add(routeId);
                }
            } // for문 종료
        }  // for문 종료

        return maxContinuousList;
    }

    private Map<String, List<Long>> makeMaxContinuousMap(Map<String, List<Long>> busInfoMap, Map<String, Long> maxMap, List<String> maxContinuousList) {
        Map<String, List<Long>> maxContinuousMap = new TreeMap<>();
        List<String> originRouteIdList = maxMap.keySet().stream().sorted().toList();

        for (String originRouteId : originRouteIdList) {
            String minRouteId = "null";
            List<Long> minSequenceList = new ArrayList<>();
            int minSize = 999;

            for (String routeId : maxContinuousList) {

                // 포함하지 않는다면 다음 originRoueId로 넘어가기
                if (routeId.contains(originRouteId)) {
                    // 분리하였을때 분리되는 개수 구하기
                    List<Long> sequenceList = busInfoMap.get(routeId);

                    List<List<Long>> dividedContinuousList = checkSequential(sequenceList);

                    // 가장 적은 개수의 routeId만 사용하여 maxContinuousMap에 삽입
                    if (dividedContinuousList.size() < minSize) {
                        minRouteId = routeId;
                        minSequenceList = sequenceList;
                        minSize = dividedContinuousList.size();
                    }
                } // if 문 종료
            } // for문 종료

            if (minRouteId.equals("null") || minSize == 999 || minSequenceList.isEmpty()) {
                throw new RuntimeException("makeMaxContinuousMap에서 에러 발생");
            }

            maxContinuousMap.put(minRouteId, minSequenceList);

        } // for문 종료

        return maxContinuousMap;
    }

    private Long calculateContinuousCount(List<Long> sequenceList) {
        // ex) sequenceList = [19] -> 0
        // ex) sequenceList = [22,25] -> 0
        // ex) sequenceList = [1,2,3,4,4,6,9] -> 4
        // ex) sequenceList = [1,3,4,5,9,13,21,22] -> 5
        // ex) sequenceList = [1,3,2,4,7,1,0,1,2,3,3,2,6] -> 4
        // ex) sequenceList = [1,2,3,7,8,23,24,1,3] -> 7
        // 연속되는 부분이 있다면 해당 sequence 값을 continuousSequenceList 리스트에 넣기

        Long totalCount = 0L;
        Long currentCount = 1L;
        for (int i = 1; i < sequenceList.size(); i++) {
            Long currentSequence = sequenceList.get(i);
            Long previousSequence = sequenceList.get(i - 1);

            // 연속된 숫자라면
            if (currentSequence.equals(previousSequence + 1)) {
                currentCount++;
                continue;
            }

            // 연속이 끊어졌다면 그 길이를 합산하고 currentCount 초기화
            if (currentCount > 1) {
                totalCount += currentCount;
            }
            currentCount = 1L;  // 새로운 구간 시작
        }

        // 마지막 구간도 확인
        if (currentCount > 1) {
            totalCount += currentCount;
        }

        return totalCount;
    }

    private List<List<Long>> checkSequential(List<Long> sequenceList) {
        List<List<Long>> continuousSequenceList = new ArrayList<>();

        // ex) sequenceList = [1,2,3,4,4,6,9] -> continuousSequenceList = [[1,2,3,4]]
        // ex) sequenceList = [1,3,4,5,9,13,21,22] -> continuousSequenceList = [[3,4,5], [21,22]]
        // ex) sequenceList = [1,3,2,4,7,1,0,1,2,3,3,2,6] -> continuousSequenceList = [[0,1,2,3]]
        // ex) sequenceList = [1,2,3,7,8,23,24,1,3] -> continuousSequenceList = [[1,2,3], [7,8], [23, 24]]
        // 연속되는 부분이 있다면 해당 sequence 값을 continuousSequenceList 리스트에 넣기

        List<Long> continuousSequences = new ArrayList<>(List.of(sequenceList.get(0)));

        for (int i = 1; i < sequenceList.size(); i++) {
            Long currentSequence = sequenceList.get(i);
            Long previousSequence = sequenceList.get(i - 1);

            if (currentSequence.equals(previousSequence + 1)) {
                continuousSequences.add(currentSequence);
                continue;
            }

            addContinuousSequencesIfValid(continuousSequenceList, continuousSequences);
            continuousSequences = new ArrayList<>(List.of(currentSequence));
        }

        addContinuousSequencesIfValid(continuousSequenceList, continuousSequences);

        return continuousSequenceList;
    }

    private List<String> checkDuplicateRouteId(List<String> oneStationInfoList) {
        Map<String, StringBuilder> oneStationInfoMap = new TreeMap<>();

        for (String oneBusInfo : oneStationInfoList) {
            List<String> routeIdAndSequence = Arrays.asList(oneBusInfo.split(","));
            String routeId = routeIdAndSequence.get(0);
            String sequence = routeIdAndSequence.get(1);

            // 같은 routeId가 있으면 "000"을 추가하여 결합
            oneStationInfoMap.putIfAbsent(routeId, new StringBuilder());
            if (!oneStationInfoMap.get(routeId).isEmpty()) {
                oneStationInfoMap.get(routeId).append("000");
            }
            oneStationInfoMap.get(routeId).append(sequence);
        }

        List<String> checkedOneStationInfoList = new ArrayList<>();
        for (var oneBusInfo : oneStationInfoMap.entrySet()) {
            String routeId = oneBusInfo.getKey();
            String sequence = oneBusInfo.getValue().toString();
            checkedOneStationInfoList.add(routeId + "," + sequence);
        }

        return checkedOneStationInfoList;
    }

    private boolean check000(Map<String, List<Long>> busInfoMap) {
        for (List<Long> sequenceList : busInfoMap.values()) {
            for (Long sequence : sequenceList) {
                if (String.valueOf(sequence).contains("000")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Long findMaxCount(Map<String, List<Long>> updatedBusInfoMap, String pureRouteId) {
        Set<String> keySet = updatedBusInfoMap.keySet();
        ArrayList<Long> countList = new ArrayList<>();

        for (String routeId : keySet) {
            if (routeId.contains(pureRouteId)) {
                List<String> routeIdAndCount = Arrays.asList(routeId.split("_"));
                countList.add(Long.parseLong(routeIdAndCount.get(1)));
            }
        }
        return countList.stream().max(Long::compareTo).get();
    }

    private void addContinuousSequencesIfValid(List<List<Long>> continuousSequenceList, List<Long> continuousSequences) {
        if (continuousSequences.size() > 1) {
            continuousSequenceList.add(continuousSequences);
        }
    }

    private String assignKeyName(Map<String, List<Long>> continuousBusInfoMap, String routeId) {
        String keyName = "";

        if (routeId.contains("_")) {
            List<String> routeIdAndCount = Arrays.asList(routeId.split("_"));
            routeId = routeIdAndCount.get(0);
        }

        // 만약 있으면 _?, 없으면 _1
        Set<String> keySet = continuousBusInfoMap.keySet();

        // routeId가 포함된 패턴
        String pattern = ".*" + routeId + ".*";

        if (keySet.stream().anyMatch(id -> id.matches(pattern))) {
            Long count = findMaxCount(continuousBusInfoMap, routeId);
            keyName = routeId + "_" + (count+1);
        }

        if (keySet.stream().noneMatch(id -> id.matches(pattern))) {
            keyName = routeId + "_1";
        }

        return keyName;
    }

    private Long stringToLong(String string) {
        return Long.parseLong(string);
    }

    private List<GeofenceLocation> sortByUserFenceInTime(List<GeofenceLocation> geofenceLocationList) {
        return geofenceLocationList.stream()
                .sorted(Comparator.comparing(GeofenceLocation::userFenceInTime))
                .toList();
    }

}
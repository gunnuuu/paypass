package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;

import java.util.*;

@Slf4j
@Service
public class BasicAlgorithmService {

    public List<GeofenceLocation> algorithmStart(List<GeofenceLocation> geofenceLocations) {
        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = sortByUserFenceInTime(geofenceLocations);

        Map<String, List<Integer>> indexMap = basicLogic(sortedGeofenceLocations);

        // indexMap을 참고하여 geofenceLocation 필터링
        List<GeofenceLocation> geofenceLocationList = filterGeofenceLocationList(sortedGeofenceLocations, indexMap);

        // list를 set으로 변환해서 test하기 (테스트용도)
        for (GeofenceLocation geofenceLocation : geofenceLocationList) {
            System.out.println("geofenceLocation = " + geofenceLocation);
        }
        System.out.println("geofenceLocationList.size() = " + geofenceLocationList.size());

        Set<GeofenceLocation> geofenceLocationSet = Set.copyOf(geofenceLocationList);
        List<GeofenceLocation> resultList = new ArrayList<>(geofenceLocationSet);
        List<GeofenceLocation> sortedResultList = sortByUserFenceInTime(resultList);

        System.out.println("------------------------------------------------------------------");
        System.out.println("중복 제거 후 정렬한 리스트");
        for (GeofenceLocation geofenceLocation : sortedResultList) {
            System.out.println("geofenceLocation = " + geofenceLocation);
        }
        System.out.println("sortedresultList.size() = " + sortedResultList.size());

        return sortedResultList;
    }

    private Map<String, List<Integer>> basicLogic(List<GeofenceLocation> sortedGeofenceLocations) {

        // busInfo만 존재하는 List 생성
        List<String> busInfoList = makeBusInfoList(sortedGeofenceLocations);
        log.info("busInfoList = " + busInfoList);

        // busInfoList를 통하여 busInfoMap 생성
        Map<String, List<Long>> busInfoMap = makeBusInfoMap(busInfoList);
        log.info("busInfoMap = " + busInfoMap);

        // 여기서 하나의 busInfo 안에 두개의 routeId가 있는 경우 해결
        // ex) 700013, 1400034, 1000030 이런식으로 변형? 000이 포함되어 있으면 routeId 중복 상황이다.
        Map<String, List<Long>> checkedBusInfoMap = checkDuplicateRouteId(busInfoMap);
        log.info("checkedBusInfoMap = " + checkedBusInfoMap);

        // sequence가 순차적으로 증가하는지 검사
        // sequence의 일정 부분만 조건 만족 시 해당 부분의 index만 추출
        // 조건 만족 시 해당 beginIndex와 endIndex를 가지는 map 추가
        Map<String, List<Integer>> indexMap = makeIndexMap(checkedBusInfoMap);
        log.info("indexMap = " + indexMap);

        return indexMap;
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
        Map<String, List<Long>> busInfoMap = new HashMap<>();

        // busInfoList에서 routeId만을 추출해서 set 생성
        Set<String> routeIdSet = makeRouteIdSet(busInfoList);

        // busInfoMap에서 key값 추가
        busInfoMapPlusKey(busInfoMap, routeIdSet);

        // busInfoMap에서 value값 추가
        busInfoMapPlusValue(busInfoMap, busInfoList);

        return busInfoMap;
    }

    private Map<String, List<Integer>> makeIndexMap(Map<String, List<Long>> busInfoMap) {
        Map<String, List<Integer>> indexMap = new HashMap<>();

        for (String routeId : busInfoMap.keySet()) {
            List<Long> sequenceList = busInfoMap.get(routeId);
            List<Integer> beginAndEndIndex = checkSequential(sequenceList);

            // 조건 만족시 실행 로직
            if (beginAndEndIndex.size() >= 2) {
                indexMap.put(routeId, beginAndEndIndex);
            }

            // 조건 불만족 시 실행 로직
            if (beginAndEndIndex.isEmpty()) {
                continue;
            }
        }

        return indexMap;
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
            // oneStationInfoList를 쪼개서 routeIdAndSequence 생성
            for (String oneBusInfo : oneStationInfoList) {
                List<String> routeIdAndSequence = Arrays.asList(oneBusInfo.split(","));
                String routeId = routeIdAndSequence.get(0);
                Long sequence = stringToLong(routeIdAndSequence.get(1));
                // sequence 값 추가하기
                List<Long> sequenceList = busInfoMap.get(routeId);
                sequenceList.add(sequence);
            }
        }
    }

    private List<Integer> checkSequential(List<Long> sequenceList) {
        List<Integer> beginAndEndIndex = new ArrayList<>();

        // ex) sequenceList = [1,2,3,4,4,6,9] -> beginAndEndIndex = [0,3]
        // ex) sequenceList = [1,3,4,5,9,13,21,22] -> beginAndEndIndex = [1,3,6,7]
        // ex) sequenceList = [1,3,2,4,7,1,0,1,2,3,3,2,6] -> beginAndEndIndex = [6,9]
        // ex) sequenceList = [1,2,3,7,8,23,24,1,3] -> beginAndEndIndex = [0,2,3,4,5,6]
        // 연속되는 부분이 있다면 해당 index 값을 beginAndEndIndex 리스트에 넣기
        if (sequenceList.size() < 2) {
            return beginAndEndIndex;
        }

        int startIndex = 0;
        // 연속되는 구간의 시작과 끝 여부를 판단
        boolean isSequential = false;

        for (int i = 0; i < sequenceList.size() - 1; i++) {
            // 두 값이 연속적인지 여부를 판단
            boolean isContinuous = sequenceList.get(i + 1) - sequenceList.get(i) == 1;

            // i+1 값과 연속성 존재 + 첫 연속 구간
            if (isContinuous && !isSequential) {
                startIndex = i;
                isSequential = true;
            }

            // i+1 값과 연속성 존재하지 않음 + 연속구간 끝
            if (!isContinuous && isSequential) {
                beginAndEndIndex.add(startIndex);
                beginAndEndIndex.add(i);
                isSequential = false;
            }
        }

        if (isSequential) {
            beginAndEndIndex.add(startIndex);
            beginAndEndIndex.add(sequenceList.size() - 1);
        }

        return beginAndEndIndex;
    }

    private List<GeofenceLocation> filterGeofenceLocationList(List<GeofenceLocation> sortedgeofenceLocationList, Map<String, List<Integer>> indexMap) {
        List<GeofenceLocation> geofenceLocationList = new ArrayList<>();
        Set<String> keySet = indexMap.keySet();

        for (String routeId : keySet) {
            List<GeofenceLocation> containRouteIdList = new ArrayList<>();
            List<Integer> indexList = indexMap.get(routeId);

            // sortedGeofenceLocationList에서 geofenceLocation을 추출해서 하나하나 확인하기
            for (GeofenceLocation geofenceLocation : sortedgeofenceLocationList) {
                // 만약 포함한지 않는다면
                if (!geofenceLocation.stationBusInfo().contains(routeId)) {
                    continue;
                }

                // 만약 포함한다면
                if (geofenceLocation.stationBusInfo().contains(routeId)) {
                    containRouteIdList.add(geofenceLocation);
                }
            }

            // indexList에 존재하는 index를 활용하여 containList indexing하기
            List<GeofenceLocation> sequentialcontainRouteIdList = indexcontainRouteIdList(containRouteIdList, indexList);
            geofenceLocationList.addAll(sequentialcontainRouteIdList);
        }

        return geofenceLocationList;
    }

    private List<GeofenceLocation> indexcontainRouteIdList(List<GeofenceLocation> containRouteIdList, List<Integer> indexList) {
        List<GeofenceLocation> sequentialcontainRouteIdList = new ArrayList<>();
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        for (GeofenceLocation geofenceLocation : containRouteIdList) {
            System.out.println("containRouteIdList의 geofenceLocation = " + geofenceLocation);
        }
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        for (int i = 0; i < indexList.size(); i += 2) {
            Integer startIndex = indexList.get(i);
            Integer endIndex = indexList.get(i + 1);

            List<GeofenceLocation> localList = containRouteIdList.subList(startIndex, endIndex + 1);

            System.out.println("==========================================================");
            for (GeofenceLocation geofenceLocation : localList) {
                System.out.println("localList의 geofenceLocation = " + geofenceLocation);
            }
            System.out.println("==========================================================");
            sequentialcontainRouteIdList.addAll(localList);
        }
        return sequentialcontainRouteIdList;
    }

    private Map<String,List<Long>> checkDuplicateRouteId(Map<String,List<Long>> busInfoMap) {
        Map<String,List<Long>> checkedBusInfoMap = new HashMap<>();




        return checkedBusInfoMap;
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
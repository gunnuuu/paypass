package project.paypass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmService {

    public void basic(List<GeofenceLocation> geofenceLocations) {
        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = geofenceLocations.stream()
                .sorted(Comparator.comparing(GeofenceLocation::userFenceInTime))
                .toList();

        basicLogic(sortedGeofenceLocations);
    }

    private void basicLogic(List<GeofenceLocation> sortedGeofenceLocations) {

        // busInfo만 존재하는 List 생성
        List<String> busInfoList = makeBusInfoList(sortedGeofenceLocations);
        log.info("busInfoList = " + busInfoList);

        // busInfoList를 통하여 busInfoMap 생성
        Map<String, List<Long>> busInfoMap = makeBusInfoMap(busInfoList);
        log.info("busInfoMap = " + busInfoMap);

        // sequence가 순차적으로 증가하는지 검사
        // sequence의 일정 부분만 조건 만족 시 해당 부분의 index만 추출
        // 조건 만족 시 해당 beginIndex와 endIndex를 가지는 map 추가
        Map<String, List<Integer>> indexMap = makeIndexMap(busInfoMap);
        log.info("indexMap = " + indexMap);

        // 이후 해당 indexMap을 활용하여 geofenceLocation 필터링 후 평균시간비교 로직 작동

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

        if (sequenceList.size() < 2) {
            return beginAndEndIndex;
        }




        return beginAndEndIndex;
    }


    private Long stringToLong(String string) {
        return Long.parseLong(string);
    }

}
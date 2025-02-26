package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.Tree;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DuplicateDeleteAlgorithm {

    // 평균 시간 데이터를 통과하고 난 이후의 geofenceLocation 데이터만 들어온다.
    public List<GeofenceLocation> algorithmStart(List<GeofenceLocation> geofenceLocations, Map<String, List<Long>> averageTimeMap) {

        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = sortByUserFenceInTime(geofenceLocations);

        // routeId : List<> stationNumber 형식의 Map 작성
        Map<String, List<Long>> stationMap = makeStationMap(sortedGeofenceLocations, averageTimeMap);
        log.info("stationMap = " + stationMap);

        // routeId: Map<> fenceInTime, fenceOutTime 형식의 Map 작성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = makeTimeMap(sortedGeofenceLocations, stationMap);
        log.info("timeMap = " + timeMap);

        // stationMap 정류장 중복 제거 알고리즘 작동 (시간체크필요)


        // stationMap 정류장 포함 제거 알고리즘 작동 (시간체크필요)


        return null;
    }

    private Map<String, List<Long>> makeStationMap(List<GeofenceLocation> geofenceLocations, Map<String, List<Long>> averageTimeMap) {
        Map<String, List<Long>> stationMap = new TreeMap<>(averageTimeMap);

        // routedId와 sequence를 활용하여 해당 geofenceLocation의 stationNumber 찾기
        for (var routeIdAndSequences : stationMap.entrySet()) {
            List<Long> stationNumberList = new ArrayList<>();

            String routeId = routeIdAndSequences.getKey();
            List<Long> sequences = routeIdAndSequences.getValue();

            String pureRouteId = routeIdToPureRouteId(routeId);

            for (Long sequence : sequences) {
                String oneBusInfo = "{" + pureRouteId + "," + sequence + "}";

                for (GeofenceLocation geofenceLocation : geofenceLocations) {
                    if (geofenceLocation.stationBusInfo().contains(oneBusInfo)) {
                        Long stationNumber = geofenceLocation.getStationNumber();
                        stationNumberList.add(stationNumber);
                        break;
                    }
                }
            }

            stationMap.put(routeId, stationNumberList);
        }
        return stationMap;
    }

    private Map<String, List<Map<String, LocalDateTime>>> makeTimeMap(List<GeofenceLocation> geofenceLocations, Map<String, List<Long>> stationMap) {
        // route별 geofenceLocation Map 생성
        Map<String, List<GeofenceLocation>> geofenceLocationMap = makeGeofenceLocationMap(geofenceLocations);
        log.info("geofenceLocationMap = " + geofenceLocationMap);

        // 인자가 하나인 경우 삭제 +
        // stationMap과 똑같은 모양의 routeId = List<GeofenceLocation> 형식의 Map 생성
        Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap = makeContinuousGeofenceLocationMap(geofenceLocationMap, stationMap);
        System.out.println("continuousGeofenceLocationMap 생성 및 routedId&stationNumber 중복된 stationMap 삭제");
        log.info("stationMap = " + stationMap);
        log.info("continuousGeofenceLocationMap = " + continuousGeofenceLocationMap);

        // 해당 Map에서 userFenceInTime과 userFenceOutTime을 활용한 Map 생성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = transformGeofenceLocationToTime(continuousGeofenceLocationMap);

        return timeMap;
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

    private Map<String, List<GeofenceLocation>> makeContinuousGeofenceLocationMap(Map<String, List<GeofenceLocation>> geofenceLocationMap, Map<String, List<Long>> stationMap) {
        Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap = new TreeMap<>();
        List<String> deleteKeyList = new ArrayList<>();

        for (var routeIdAndGeofenceLocationList : geofenceLocationMap.entrySet()) {
            String routeId = routeIdAndGeofenceLocationList.getKey();
            List<GeofenceLocation> geofenceLocationList = routeIdAndGeofenceLocationList.getValue();

            int deleteStartIndex = 0;

            String pattern = ".*" + routeId + ".*";

            if (geofenceLocationList.size() < 2) {
                continue;
            }

            List<Long> stationNumberList = geofenceLocationList.stream()
                    .map(GeofenceLocation::getStationNumber)
                    .collect(Collectors.toList());

            for (var routeIdAndStationNumberList : stationMap.entrySet()) {
                String routeIdInStationMap = routeIdAndStationNumberList.getKey();
                List<Long> stationNumberListInStationMap = routeIdAndStationNumberList.getValue();

                if (routeIdInStationMap.matches(pattern)) {
                    // 있다면 continuousGeofenceLocationMap에 추가 + stationNumberList에서 값 삭제
                    if (Collections.indexOfSubList(stationNumberList,stationNumberListInStationMap) != -1) {

                        int startIndex = Collections.indexOfSubList(stationNumberList, stationNumberListInStationMap);

                        continuousGeofenceLocationMap.put(routeIdInStationMap,geofenceLocationList.subList(startIndex + deleteStartIndex, startIndex + deleteStartIndex + stationNumberListInStationMap.size()));

                        stationNumberList.subList(startIndex, startIndex + stationNumberListInStationMap.size()).clear();

                        deleteStartIndex += stationNumberListInStationMap.size();

                        continue;
                    }

                    // stationNumberList에는 없는데 stationMap에는 있는 경우
                    // -> stationMap 삭제 (routeId와 stationNumber가 겹치는 경우이다.)
                    if (Collections.indexOfSubList(stationNumberList, stationNumberListInStationMap) == -1) {
                        deleteKeyList.add(routeIdInStationMap);
                    }
                } // match if문 종료
            } // for문 종료
        }

        for (String routeId : deleteKeyList) {
            stationMap.remove(routeId);
        }

        return continuousGeofenceLocationMap;
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

    private String routeIdToPureRouteId(String routeId) {
        if (routeId.contains("_")) {
            List<String> routeIdAndCount = Arrays.asList(routeId.split("_"));
            routeId = routeIdAndCount.get(0);
        }
        return routeId;
    }

    private List<GeofenceLocation> sortByUserFenceInTime(List<GeofenceLocation> geofenceLocationList) {
        return geofenceLocationList.stream()
                .sorted(Comparator.comparing(GeofenceLocation::userFenceInTime))
                .toList();
    }
}
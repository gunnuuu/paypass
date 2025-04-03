package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DuplicateDeleteAlgorithm {

    // 평균 시간 데이터를 통과하고 난 이후의 geofenceLocation 데이터만 들어온다.
    public Map<List<GeofenceLocation>, List<String>> algorithmStart(Map<String, List<Long>> averageTimeMap, List<GeofenceLocation> geofenceLocations) {

        // 데이터 정렬(fenceInTime 기준)
        List<GeofenceLocation> sortedGeofenceLocations = sortByUserFenceInTime(geofenceLocations);

        // routeId : List<> stationNumber 형식의 Map 작성
        Map<String, List<Long>> stationMap = makeStationMap(sortedGeofenceLocations, averageTimeMap);
        log.info("stationMap = " + stationMap);

        // routeId : List<> geofenceLocation 형식의 Map 작성
        // route별 geofenceLocation Map 생성
        Map<String, List<GeofenceLocation>> geofenceLocationMap = makeGeofenceLocationMap(sortedGeofenceLocations);
        log.info("geofenceLocationMap = " + geofenceLocationMap);

        // 인자가 하나인 경우 삭제 +
        // stationMap과 똑같은 모양의 routeId = List<GeofenceLocation> 형식의 Map 생성
        Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap = makeContinuousGeofenceLocationMap(geofenceLocationMap, stationMap);
        log.info("################################# continuousGeofenceLocationMap 생성 및 routedId&stationNumber 중복된 stationMap 삭제 #################################");
        log.info("stationMap = " + stationMap);
        log.info("continuousGeofenceLocationMap = " + continuousGeofenceLocationMap);

        // routeId: Map<> fenceInTime, fenceOutTime 형식의 Map 작성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = makeTimeMap(continuousGeofenceLocationMap);
        log.info("timeMap = " + timeMap);

        // ** 현재 stationMap과 continuousGeofenceLocationMap과 timeMap은 같은 유형이다. (value의 유형만 다르다)
        // stationMap 정류장 포함 제거 알고리즘 작동 (시간체크필요)
        // stationNumberList가 같지는 않지만 포함하고 시간도 포함하는 관게면 삭제
        deleteContainStation(stationMap, timeMap, continuousGeofenceLocationMap);
        log.info("################################# deleteContainStation method가 작동한 이후 #################################");
        log.info("stationMap = " + stationMap);
        log.info("timeMap = " + timeMap);

        // 지금까지 만든 Map 기반으로 geofenceLocation으로 된 최종 Map 생성
        // Map<List<GeofenceLocation>, List<String>>형식으로 마무리
        // geofenceLocation : routeId = 경로 : 버스번호
        Map<List<GeofenceLocation>, List<String>> resultMap = makeResultMap(continuousGeofenceLocationMap, stationMap, timeMap);
        log.info("resultMap = " + resultMap);

        return resultMap;
    }

    private Map<List<GeofenceLocation>, List<String>> makeResultMap(Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap, Map<String, List<Long>> stationMap, Map<String, List<Map<String, LocalDateTime>>> timeMap) {
        Map<List<GeofenceLocation>, List<String>> resultMap = new HashMap<>();
        log.info("continuousGeofenceLocationMap = " + continuousGeofenceLocationMap);

        if (!stationMap.keySet().equals(timeMap.keySet())) throw new RuntimeException("makeResultMap method 실행 시 stationMap과 timeMap의 KeySet이 일치하지 않습니다.");

        for (var routeIdAndGeofenceLocationList : continuousGeofenceLocationMap.entrySet()) {
            String routeId = routeIdAndGeofenceLocationList.getKey();
            String pureRouteId = Arrays.asList(routeId.split("_")).get(0);
            List<GeofenceLocation> geofenceLocationList = routeIdAndGeofenceLocationList.getValue();

            // resultMap에 이미 존재한다면 List에 routeId를 추가한다.
            if (checkDuplicateGeofenceLocation(resultMap, geofenceLocationList)){
                List<String> originList = resultMap.get(geofenceLocationList);
                originList.add(pureRouteId);
            }

            // resultMap에 존재하지 않는다면 List를 생성하고 추가한다.
            if (!checkDuplicateGeofenceLocation(resultMap, geofenceLocationList)){
                ArrayList<String> routeIdList = new ArrayList<>();
                routeIdList.add(pureRouteId);
                resultMap.put(geofenceLocationList, routeIdList);
            }

        }
        return resultMap;
    }

    private boolean checkDuplicateGeofenceLocation(Map<List<GeofenceLocation>, List<String>> resultMap, List<GeofenceLocation> geofenceLocationList) {
        Set<List<GeofenceLocation>> keySet = resultMap.keySet();
        for (List<GeofenceLocation> geofenceLocations : keySet) {
            if (geofenceLocations.equals(geofenceLocationList)){
                return true;
            }
        }
        return false;
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

    private Map<String, List<Map<String, LocalDateTime>>> makeTimeMap(Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap) {
        // 해당 Map에서 userFenceInTime과 userFenceOutTime을 활용한 Map 생성
        Map<String, List<Map<String, LocalDateTime>>> timeMap = transformGeofenceLocationToTime(continuousGeofenceLocationMap);

        return timeMap;
    }

    private Map<String, List<GeofenceLocation>> makeContinuousGeofenceLocationMap(Map<String, List<GeofenceLocation>> geofenceLocationMap, Map<String, List<Long>> stationMap) {
        Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap = new TreeMap<>();
        List<String> deleteKeyList = new ArrayList<>();

        for (var routeIdAndGeofenceLocationList : geofenceLocationMap.entrySet()) {
            String routeId = routeIdAndGeofenceLocationList.getKey();
            List<GeofenceLocation> geofenceLocationList = routeIdAndGeofenceLocationList.getValue();

            String pattern = ".*" + routeId + ".*";

            if (geofenceLocationList.size() < 2) continue;

            List<Long> stationNumberList = makeStationNumberList(geofenceLocationList);
            List<Long> idList = makeIdList(geofenceLocationList);

            List<String> routeIdList = geofenceLocationMap.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            for (String routeIdInRouteIdList : routeIdList){
                int count = 1;

                while (true) {
                    String routeIdInStationMap = routeIdInRouteIdList + "_" + count;

                    if (!stationMap.containsKey(routeIdInStationMap)) {
                        break;
                    }

                    List<Long> stationNumberListInStationMap = stationMap.get(routeIdInStationMap);

                    if (routeIdInStationMap.matches(pattern)) {
                        // 있다면 continuousGeofenceLocationMap에 추가 + stationNumberList에서 값 삭제
                        if (Collections.indexOfSubList(stationNumberList, stationNumberListInStationMap) != -1) {
//                            System.out.println("routeIdInStationMap = " + routeId);
//                            System.out.println("stationNumberList = " + stationNumberList);
//                            System.out.println("routeIdInStationMap = " + routeIdInStationMap);
//                            System.out.println("stationNumberListInStationMap = " + stationNumberListInStationMap);
//                            System.out.println(" ");
                            int startIndex = Collections.indexOfSubList(stationNumberList, stationNumberListInStationMap);

                            // continuousGeofenceLocationMap에 값 추가
                            List<Long> idListForPut = idList.subList(startIndex, startIndex + stationNumberListInStationMap.size());
                            List<GeofenceLocation> putList = makePutList(idListForPut ,geofenceLocationList);
                            continuousGeofenceLocationMap.put(routeIdInStationMap, putList);

                            // 중복 방지를 위해 List에서 확인된 값 삭제
                            stationNumberList.subList(startIndex, startIndex + stationNumberListInStationMap.size()).clear();
                            idList.subList(startIndex, startIndex + stationNumberListInStationMap.size()).clear();
                            // stationMap에 존재하는 stationNumberList의 size만큼 추가 (geofenceLocationList에 적용하기 위함)
                            count++;
                            continue;
                        }

                        // stationNumberList에는 없는데 stationMap에는 있는 경우
                        // -> stationMap 삭제 (routeId와 stationNumber + 시간이 겹치는 경우)
                        if (Collections.indexOfSubList(stationNumberList, stationNumberListInStationMap) == -1) {
                            deleteKeyList.add(routeIdInStationMap);
                        }

                    } // match if문 종료

                    count++;
                }
            }

        }
        // 중복된 부분 stationMap에서 삭제
        for (String routeId : deleteKeyList) {
            stationMap.remove(routeId);
        }

        return continuousGeofenceLocationMap;
    }



    private List<Long> makeStationNumberList(List<GeofenceLocation> geofenceLocationList) {
        return geofenceLocationList.stream()
                .map(GeofenceLocation::getStationNumber)
                .collect(Collectors.toList());
    }

    private List<Long> makeIdList(List<GeofenceLocation> geofenceLocationList) {
        return geofenceLocationList.stream()
                .map(GeofenceLocation::geofenceLocationId)
                .collect(Collectors.toList());
    }

    private List<GeofenceLocation> makePutList(List<Long> idListForPut, List<GeofenceLocation> geofenceLocationList) {
        List<GeofenceLocation> putList = geofenceLocationList.stream()
                .filter(geofenceLocation -> idListForPut.contains(geofenceLocation.geofenceLocationId())) // ID가 포함된 경우만 필터링
                .collect(Collectors.toList()); // 리스트로 변환

        if (putList.size() != idListForPut.size()) throw new RuntimeException("makePutList에서 오류 발생");

        return putList;
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

    private void deleteContainStation(Map<String, List<Long>> stationMap, Map<String, List<Map<String, LocalDateTime>>> timeMap, Map<String, List<GeofenceLocation>> continuousGeofenceLocationMap) {
        // 삭제할 key 값을 담을 리스트
        ArrayList<String> deleteKeyList = new ArrayList<>();

        for (var routeIdAndStationNumber : stationMap.entrySet()) {
            String routeId = routeIdAndStationNumber.getKey();
            List<Long> stationNumberList = routeIdAndStationNumber.getValue();

            for (var routeIdAndStationNumberInner : stationMap.entrySet()) {
                String routeIdInner = routeIdAndStationNumberInner.getKey();
                List<Long> stationNumberListInner = routeIdAndStationNumberInner.getValue();
                // 포함은 하지만 동일하지 않는 경우
                if (Collections.indexOfSubList(stationNumberList, stationNumberListInner) != -1
                        && !stationNumberList.equals(stationNumberListInner)) {
                    // 시간도 포함하는지 확인
                    String deleteKey = checkTimeInContain(routeId, routeIdInner, timeMap);
                    deleteKeyList.add(deleteKey);
                } // end if
            }
        } // end for

        for (String routeId : deleteKeyList) {
            if (routeId == null) throw new RuntimeException("checkTimeInContain 메서드에서 오류 발생");
            if (routeId.equals("-1")) continue;
            stationMap.remove(routeId);
            timeMap.remove(routeId);
            continuousGeofenceLocationMap.remove(routeId);
        }
    }

    private String checkTimeInContain(String routeId, String routeIdInner, Map<String, List<Map<String, LocalDateTime>>> timeMap) {
        // 해당 routeId의 시간을 비교해서 시간마저 포함한다면 삭제 대상
        // 비교는 가장 처음 값의 fenceInTime과 가장 마지막 값의 fenceOutTime으로 비교한다.
        int listSizeOuter = timeMap.get(routeId).size();
        LocalDateTime fenceInTimeOuter = timeMap.get(routeId).get(0).get("fenceInTime");
        LocalDateTime fenceOutTimeOuter = timeMap.get(routeId).get(listSizeOuter - 1).get("fenceOutTime");

        int listSizeInner = timeMap.get(routeIdInner).size();
        LocalDateTime fenceInTimeInner = timeMap.get(routeIdInner).get(0).get("fenceInTime");
        LocalDateTime fenceOutTimeInner = timeMap.get(routeIdInner).get(listSizeInner - 1).get("fenceOutTime");
        // fenceOutTime이 null인 경우는?
        // 1. Outer가 null Inner도 null -> 포함했다고 판단
        // 3. Outer는 null Inner는 인자값 -> fenceInTime 비교 필요
        // 2. Outer가 인자값 Inner은 null -> Inner가 더 최근인 경우 -> 스킵
        // 4. 둘 다 인자값 -> 포함 여부 확인 필요

//        log 확인용
//        System.out.println("routeId = " + routeId);
//        System.out.println("fenceInTimeOuter = " + fenceInTimeOuter);
//        System.out.println("fenceOutTimeOuter = " + fenceOutTimeOuter);
//        System.out.println("routeIdInner = " + routeIdInner);
//        System.out.println("fenceInTimeInner = " + fenceInTimeInner);
//        System.out.println("fenceOutTimeInner = " + fenceOutTimeInner);

        // 1.
        if (fenceOutTimeOuter == null && fenceOutTimeInner == null) {
            return routeIdInner;
        }

        // 2
        if (fenceOutTimeOuter == null && fenceOutTimeInner != null) {
            return (fenceInTimeOuter.compareTo(fenceInTimeInner) > 0) ? "-1" : routeIdInner;
        }

        // 3
        if (fenceOutTimeOuter != null && fenceOutTimeInner == null) {
            return "-1";
        }

        // 4 시간을 포함한다면 삭제 리스트에 포함
        if (fenceOutTimeOuter != null && fenceOutTimeInner != null) {
            if (fenceInTimeOuter.compareTo(fenceInTimeInner) <= 0 && fenceOutTimeOuter.compareTo(fenceOutTimeInner) >= 0) {
                return routeIdInner;
            }
            // 시간을 포함하지 않는다면 스킵
            if (fenceInTimeOuter.compareTo(fenceOutTimeInner) > 0 || fenceOutTimeOuter.compareTo(fenceInTimeInner) < 0){
                return "-1";
            }
        }

        return null;
    }

    // makeResultMap 자리


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
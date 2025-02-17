package project.paypass.service;

import org.apache.commons.lang3.tuple.Pair;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;
import project.paypass.repository.GeofenceLocationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GeofenceService {

    private final GeofenceLocationRepository geofenceLocationRepository;

    @Transactional
    public GeofenceLocation createGeofenceLocation(String mainId, Long stationNumber, String busInfo) {
        return new GeofenceLocation(mainId, stationNumber, busInfo);
    }

    @Transactional
    public void save(GeofenceLocation geofenceLocation) {
        geofenceLocationRepository.save(geofenceLocation);
    }

    @Transactional
    public List<GeofenceLocation> findByMainIdAndStationNumber(String mainId, Long stationNumber) {
        return geofenceLocationRepository.findByMainIdAndStationNumber(mainId, stationNumber);
    }

    @Transactional
    public void userFenceOut(GeofenceLocation geofenceLocation) {
        geofenceLocation.userFenceOut();
    }

    @Transactional
    public boolean fenceOutTimeIsNull(GeofenceLocation geofenceLocation) {
        return geofenceLocation.fenceOutTimeIsNull();
    }

    public void startAlgorithm(String mainId) {
        List<String> busInfoList = geofenceLocationRepository.findBusInfoByMainId(mainId);
        List<Pair<Integer, Integer>> check = parseBusInfo(busInfoList);

        System.out.println("Parsed Bus Info: " + check);  // 디버깅: 파싱된 버스 정보 출력

        List<Pair<Integer, Integer>> possible = new ArrayList<>();
        Map<Integer, Integer> lastSequenceMap = new HashMap<>();

        // 모든 구간을 possible에 추가
        for (Pair<Integer, Integer> current : check) {
            int routeId = current.getLeft();
            int sequence = current.getRight();

            // 처음 들어오는 경우에는 무조건 possible에 추가
            if (!lastSequenceMap.containsKey(routeId)) {
                possible.add(current);
                lastSequenceMap.put(routeId, sequence);
            } else {
                // 연속된 sequence인지 확인
                if (lastSequenceMap.get(routeId) + 1 == sequence) {
                    possible.add(current);
                }
                lastSequenceMap.put(routeId, sequence);
            }
        }

        System.out.println("Possible Combinations: " + possible);  // 디버깅: 가능한 조합 출력

        // 1️⃣ sequence가 1증가하고, 5분 이내 이동 시간을 만족하는 구간을 찾기 위해 정렬
        possible.sort(Comparator.comparingInt((Pair<Integer, Integer> p) -> p.getLeft()).thenComparingInt(Pair::getRight));

        List<Pair<Integer, Integer>> finalSequences = new ArrayList<>();

        // 2️⃣ sequence가 1씩 증가하고, 이동 시간이 5분 이내인 구간만 final에 추가
        for (int i = 0; i < possible.size() - 1; i++) {
            Pair<Integer, Integer> prev = possible.get(i);
            Pair<Integer, Integer> curr = possible.get(i + 1);

            int routeId = prev.getLeft();
            int prevSequence = prev.getRight();
            int currSequence = curr.getRight();

            // 이전 정류장 진출 시간, 현재 정류장 진입 시간 조회
            List<LocalDateTime> prevExitTimes = geofenceLocationRepository.findExitTime(mainId, routeId, prevSequence);
            List<LocalDateTime> currEntryTimes = geofenceLocationRepository.findEntryTime(mainId, routeId, currSequence);

            System.out.println("Checking routeId=" + routeId + ", prevSequence=" + prevSequence + ", currSequence=" + currSequence);  // 디버깅: 현재 검토 중인 routeId, sequence 출력

            if (prevExitTimes.isEmpty() || currEntryTimes.isEmpty()) {
                System.out.println("No exit or entry times found for this combination.");  // 디버깅: 진입/진출 시간이 없을 경우
                continue;
            }

            // 가장 최근의 진입/진출 시간 가져오기
            LocalDateTime prevExitTime = prevExitTimes.get(0);
            LocalDateTime currEntryTime = currEntryTimes.get(0);

            System.out.println("Prev Exit Time: " + prevExitTime + ", Curr Entry Time: " + currEntryTime);  // 디버깅: 진입/진출 시간 출력

            // 실제 이동 시간 계산
            long actualTravelTime = Duration.between(prevExitTime, currEntryTime).getSeconds();
            System.out.println("Actual Travel Time: " + actualTravelTime + " seconds");  // 디버깅: 실제 이동 시간 출력

            // 평균 이동 시간과 비교하여 탑승 여부 판단
            long averageTravelTime = 300; // 예시: 5분 (추후 평균 시간으로 대체 필요)
            if (actualTravelTime <= averageTravelTime) {
                System.out.println("Valid sequence: routeId=" + routeId + ", prevSequence=" + prevSequence + " → " + currSequence);

                // 해당 구간을 finalSequences에 추가 (중복 체크)
                if (!finalSequences.contains(prev)) {
                    finalSequences.add(prev);
                }
                if (!finalSequences.contains(curr)) {
                    finalSequences.add(curr);
                }
            } else {
                System.out.println("Travel time does not match average, skipping this combination.");  // 디버깅: 평균 시간보다 긴 경우
            }
        }

        System.out.println("Final Sequences: " + finalSequences);  // 디버깅: 최종 유효 구간 출력

        // 3️⃣ 탑승 여부 판단: 최종 구간에 대해 탑승 여부를 판별
        determineBoarding(finalSequences, mainId);
    }


    private List<Pair<Integer, Integer>> parseBusInfo(List<String> busInfoList) {
        List<Pair<Integer, Integer>> check = new ArrayList<>();

        for (String busInfo : busInfoList) {
            // `{116900002,1},{116900010,7}` → "116900002,1"과 "116900010,7"로 나누기
            String[] busInfoItems = busInfo.split("},\\{");

            // 각 항목에서 중괄호 제거하고, ',' 기준으로 분리
            for (String item : busInfoItems) {
                item = item.replaceAll("[{}]", ""); // 중괄호 제거
                String[] parts = item.split(",");

                if (parts.length == 2) {
                    try {
                        int routeId = Integer.parseInt(parts[0].trim());
                        int sequence = Integer.parseInt(parts[1].trim());
                        check.add(Pair.of(routeId, sequence));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid format in busInfo: " + item);
                    }
                }
            }
        }

        return check;
    }

    private void determineBoarding(List<Pair<Integer, Integer>> finalSequences, String mainId) {
        System.out.println("Starting Boarding Algorithm");  // 디버깅: 알고리즘 시작

        for (int i = 0; i < finalSequences.size() - 1; i++) {
            Pair<Integer, Integer> prev = finalSequences.get(i);
            Pair<Integer, Integer> curr = finalSequences.get(i + 1);

            int routeId = prev.getLeft();
            int prevSequence = prev.getRight();
            int currSequence = curr.getRight();

            // 이전 정류장 진출 시간, 현재 정류장 진입 시간 조회
            List<LocalDateTime> prevExitTimes = geofenceLocationRepository.findExitTime(mainId, routeId, prevSequence);
            List<LocalDateTime> currEntryTimes = geofenceLocationRepository.findEntryTime(mainId, routeId, currSequence);

            System.out.println("Checking routeId=" + routeId + ", prevSequence=" + prevSequence + ", currSequence=" + currSequence);  // 디버깅: 현재 검토 중인 routeId, sequence 출력

            if (prevExitTimes.isEmpty() || currEntryTimes.isEmpty()) {
                System.out.println("No exit or entry times found for this combination.");  // 디버깅: 진입/진출 시간이 없을 경우
                continue;
            }

            // 가장 최근의 진입/진출 시간 가져오기
            LocalDateTime prevEntryTime = prevExitTimes.get(0);
            LocalDateTime currEntryTime = currEntryTimes.get(0);

            System.out.println("Prev Entry Time: " + prevEntryTime + ", Curr Entry Time: " + currEntryTime);  // 디버깅: 진입 시간 출력

            // 실제 이동 시간 계산
            long actualTravelTime = Duration.between(prevEntryTime, currEntryTime).getSeconds();
            System.out.println("Actual Travel Time: " + actualTravelTime + " seconds");  // 디버깅: 실제 이동 시간 출력

            // 평균 이동 시간과 비교하여 탑승 여부 판단
            long averageTravelTime = 300; // 예시: 5분 (추후 평균 시간으로 대체 필요)
            if (actualTravelTime <= averageTravelTime) {
                System.out.println("탑승 확정: routeId=" + routeId + ", sequence=" + prevSequence + " → " + currSequence);

                // GeofenceLocation 객체를 업데이트하여 board 필드에 값 설정
                GeofenceLocation geofenceLocation = geofenceLocationRepository.findByMainIdAndRouteIdAndSequence(mainId, routeId, prevSequence);
                if (geofenceLocation != null) {
                    geofenceLocation.setBoard(true);  // 탑승으로 설정
                    geofenceLocationRepository.save(geofenceLocation);  // 업데이트된 객체 저장
                    System.out.println("GeofenceLocation updated: " + geofenceLocation);  // 디버깅: 업데이트된 GeofenceLocation 출력
                }
            } else {
                System.out.println("Travel time does not match average, skipping this combination.");  // 디버깅: 평균 시간보다 긴 경우
            }
        }
    }
}

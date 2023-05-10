Feature: wj

  Scenario: Check book title

    Given library click
    When search button click
    And  search input "네모, 안녕?"
    And search click
    And click 2nd book
    Then check book title

  Scenario: Book View

    Given 라이브러리 버튼 클릭
    When 검색 버튼 클릭
    And 검색창에서 "네모, 안녕?" 입력
    And 검색창에서 검색 실행 버튼 클릭
    And 검색결과 화면에서 독서 검색결과 섹션의 1번째 항목 선택
    And 5초 대기
    And Reading continue "No"
    Then 뷰어 하단 페이지 노출 확인



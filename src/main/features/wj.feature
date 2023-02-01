Feature: wj

  Scenario: Check book title

    Given library click
    When search button click
    And  search input "네모, 안녕?"
    And search click
    And click 2nd book
    Then check book title

  Scenario: Book View

    Given library click
    When search button click
    And  search input ""
    And search click
    And click 2nd book
    And ok check click
    And check book title
    And hamburger btn click
    And book view click
    And view ok button click
    Then check display text



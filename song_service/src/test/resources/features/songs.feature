Feature: Song metadata management

  Scenario: Store and retrieve song metadata
    When I create a song with id 10, name "Yesterday" and duration "02:05"
    Then the response status is 200
    And I get the song with id 10
    And the response status is 200
    And the song name is "Yesterday"

  Scenario: Reject a song whose id already exists
    Given a song with id 11 exists
    When I create a song with id 11, name "Again" and duration "01:00"
    Then the response status is 409

  Scenario: Reject a song with a malformed duration
    When I create a song with id 12, name "Broken" and duration "2:5"
    Then the response status is 400

  Scenario: Unknown song is not found
    When I get the song with id 999
    Then the response status is 404

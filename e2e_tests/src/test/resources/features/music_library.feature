Feature: Music library through the gateway

  Scenario: Uploaded MP3 gets its metadata extracted and is removed together with it
    When I upload the sample MP3
    Then the upload succeeds and returns a resource id
    And the song metadata for that resource eventually appears
    And the song name is "E2E Song" and the artist is "E2E Artist"
    When I delete the resource
    Then the resource can no longer be retrieved
    And the song metadata can no longer be retrieved

  Scenario: Upload with a non-MP3 content type is rejected
    When I upload a file with content type "text/plain"
    Then the response status is 400

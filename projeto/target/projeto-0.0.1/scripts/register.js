document.addEventListener('DOMContentLoaded', function() {
    var registrationForm = document.getElementById('registrationForm');

    registrationForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(registrationForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });

        // Convert JSON object to string
        var jsonString = JSON.stringify(jsonData);

        // Send JSON data to the server using XHR or Fetch API
        sendDataToServer(jsonString);
    });

    function sendDataToServer(jsonData) {
        // Send jsonData to the server using XHR or Fetch API
        // Example using Fetch API:
        fetch('/rest/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(response => {
            if (response.ok) {
                return response.json().then(data => {
                    // Extract token and message from the JSON data
                    var authToken = data.authToken;
                    var message = data.message;
        
                    // Use authToken and message as needed
                    localStorage.setItem("authToken", authToken);
                    console.log('Message:', message);
                });
            } else {
                // Response is not successful
                return response.text().then(errorMessage => {
                    console.error('Error:', errorMessage);
                });
            }
        })
        .catch(error => {
            // Handle fetch error
            console.error('Fetch error:', error);
        });
    }
});
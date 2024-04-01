document.addEventListener('DOMContentLoaded', function() {
    var registrationForm = document.getElementById('registrationForm');

    registrationForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(registrationForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        jsonData["role"] = "";
        jsonData["state"] = "";

        // Convert JSON object to string
        var jsonString = JSON.stringify(jsonData);

        // Send JSON data to the server using XHR or Fetch API
        sendDataToServer(jsonString);
    });

    function sendDataToServer(jsonData) {
        fetch('https://apdc-64320.oa.r.appspot.com/rest/register/v1', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const data = await response.json();
                localStorage.setItem("authToken", data);
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error:', errorMessage);
            }
        });
    }
});
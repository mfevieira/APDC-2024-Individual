document.addEventListener('DOMContentLoaded', function() {
    var registrationForm = document.getElementById('registrationForm');

    registrationForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var password = document.getElementById('password').value;
        var confirmation = document.getElementById('confirmation').value;

        if (password != confirmation) {
            alert("Password and Confirmation password do not match. Please try again.");
            return;
        }

        var formData = new FormData(registrationForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        //jsonData["role"] = "";
        //jsonData["state"] = "";
        registerUser(JSON.stringify(jsonData));
    });

    function registerUser(jsonData) {
        fetch('https://apdc-64320.oa.r.appspot.com/rest/register/user', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const data = await response.json();
                localStorage.setItem("authToken", JSON.stringify(data));
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        });
    }
});
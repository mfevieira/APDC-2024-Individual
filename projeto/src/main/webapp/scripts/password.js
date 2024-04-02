document.addEventListener('DOMContentLoaded', function() {
    var passwordForm = document.getElementById('passwordForm');

    passwordForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var newPassword = document.getElementById('newPassword').value;
        var confirmPassword = document.getElementById('confirmPassword').value;

        if (newPassword !== confirmPassword) {
            alert("New password and confirmation do not match. Please try again.");
            return;
        }

        var formData = new FormData(registrationForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        var authToken = localStorage.getItem("authToken");
        if ( authToken == null ) {
            alert("Auth Token not found. Login again.");
            window.location.href = "login.html";
            return;
        }
        var token = JSON.parse(authToken);
        jsonData["token"] = token;
        changePassword(JSON.stringify(jsonData));
    });

    function changePassword(jsonData) {
        fetch('https://apdc-64320.oa.r.appspot.com/rest/change/user/password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                window.location.href = "user.html";
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        });
    }
});
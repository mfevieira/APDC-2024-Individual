document.addEventListener('DOMContentLoaded', function() {
    var userDataForm = document.getElementById('userDataForm');

    userDataForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userDataForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        var authToken = localStorage.getItem("authToken")
        if ( authToken == null ) {
            alert("Auth Token not found.");
            window.location.href = "login.html";
            return;
        }
        var token = JSON.parse(authToken);
        jsonData["token"] = token;
        changeUserData(JSON.stringify(jsonData));
    });

    function changeUserData(jsonData) {
        fetch('https://apdc-64320.oa.r.appspot.com/rest/change/user/data', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                console.log('Change user data: ', message);
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        });
    }
});
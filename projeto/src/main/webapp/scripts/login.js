document.addEventListener('DOMContentLoaded', function() {
    var loginForm = document.getElementById('loginForm');

    loginForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(loginForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        loginUser(JSON.stringify(jsonData));
    });

    function loginUser(jsonData) {
        fetch('https://apdc-64320.oa.r.appspot.com/rest/login/user', {
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
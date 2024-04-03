document.addEventListener('DOMContentLoaded', function() {
    var userStateForm = document.getElementById('userStateForm');

    userStateForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userStateForm);
        var jsonData = {};

        formData.forEach(function(value, key) {
            jsonData[key] = value;
        });
        var authToken = localStorage.getItem('authToken')
        if ( authToken == null ) {
            alert('Auth Token not found.');
            window.location.href = 'login.html';
            return;
        }
        var token = JSON.parse(authToken);
        jsonData['token'] = token;
        changeUserState(JSON.stringify(jsonData));
    });

    function changeUserState(jsonData) {
        fetch('/rest/user/change/state', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                console.log('Change user state: ', message);
                window.location.href = 'index.html';
            } else {
                const errorMessage = await response.text();
                alert('Fetch error: ' + errorMessage);
            }
        });
    }
});
document.addEventListener('DOMContentLoaded', function() {
    var userRoleForm = document.getElementById('userRoleForm');

    userRoleForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userRoleForm);
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
        changeUserRole(JSON.stringify(jsonData));
    });

    function changeUserRole(jsonData) {
        fetch('/rest/user/change/role', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                console.log('Change user role: ', message);
                window.location.href = 'index.html';
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        });
    }
});
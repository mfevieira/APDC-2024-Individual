document.addEventListener('DOMContentLoaded', function() {
    var userRoleForm = document.getElementById('userRoleForm');

    userRoleForm.addEventListener('submit', function(event) {
        event.preventDefault();

        var formData = new FormData(userRoleForm);
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
        changeUserRemove(JSON.stringify(jsonData));
    });

    function changeUserRemove(jsonData) {
        fetch('/rest/change/user/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                const message = await response.text();
                console.log('Remove user: ', message);
                window.location.href = "index.html";
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        });
    }
});
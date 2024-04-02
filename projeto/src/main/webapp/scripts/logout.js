document.addEventListener('DOMContentLoaded', function() {
    var logoutButton = document.getElementById('logoutButton');

    logoutButton.addEventListener('click', function(event) {
        event.preventDefault();
        var jsonData = localStorage.getItem("authToken");
        logoutUser(jsonData);
    });

    function logoutUser(jsonData) {
        fetch('/rest/logout/user', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                localStorage.removeItem("authToken");
                window.location.href = "index.html";
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        })
        .catch(error => {
            console.error('Logout error:', error);
        });
    }
});
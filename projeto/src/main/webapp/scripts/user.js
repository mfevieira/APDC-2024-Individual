document.addEventListener('DOMContentLoaded', function() {
    var tokenButton = document.getElementById('tokenButton');
    var deleteButton = document.getElementById('deleteButton');
    var confirmDeleteButton = document.getElementById('confirmDeleteButton');

    tokenButton.addEventListener('click', function(event) {
        event.preventDefault();
        var authToken = localStorage.getItem('authToken');
        if ( authToken != null ) {
            alert('Auth Token Data:\n' + authToken);
        } else {
            alert('Auth Token not found.');
        }
    });

    deleteButton.addEventListener('click', function(event) {
        event.preventDefault();
        document.getElementById('confirmDeleteButton').style.display = 'block';
    });

    confirmDeleteButton.addEventListener('click', function(event) {
        event.preventDefault();

        var authToken = localStorage.getItem('authToken');
        if ( authToken == null ) {
            alert('Auth Token not found.');
            window.location.href = 'login.html';
            return;
        }
        var token = JSON.parse(authToken);
        var username = token.username;
        var jsonData = {};
        jsonData['username'] = username;
        jsonData['token'] = token;
        deleteUser(JSON.stringify(jsonData));
    });

    function deleteUser(jsonData) {
        fetch('/rest/user/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonData
        })
        .then(async response => {
            if (response.ok) {
                localStorage.removeItem('authToken');
                window.location.href = 'index.html';
            } else {
                const errorMessage = await response.text();
                console.error('Fetch error: ', errorMessage);
            }
        })
        .catch(error => {
            console.error('User deletion error:', error.message);
        });
    }
});
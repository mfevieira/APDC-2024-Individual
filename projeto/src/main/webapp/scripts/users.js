document.addEventListener('DOMContentLoaded', function() {
    var authToken = localStorage.getItem('authToken');
    if ( authToken == null ) {
        alert('Auth Token not found. Login again.');
        window.location.href = 'login.html';
        return;
    }
    fetch('/rest/list/users', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: authToken
    })
    .then(response => response.json())
    .then(data => {
        const userListContainer = document.getElementById('userList');
        const userList = document.createElement('ul');
        data.forEach(user => {
            const listItem = document.createElement('li');
            listItem.textContent = `Username: ${user.username}, Email: ${user.email}, Name: ${user.name}`;
            userList.appendChild(listItem);
        });
        userListContainer.appendChild(userList);
    })
    .catch(error => {
        alert('Error fetching users: ' + error.message);
    });
});
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
            listItem.textContent = `Username: ${user.username}, Email: ${user.email}, Name: ${user.name}, Phone: ${user.phone}, Profile: ${user.profile}, 
                                    Work: ${user.work}, Workplace: ${user.workplace}, Address: ${user.address}, Postal Code: ${user.postalcode}, 
                                    Fiscal: ${user.fiscal}, Role: ${user.role}, State: ${user.state}, User Creation Time: ${user.userCreationTime}, 
                                    TokenID: ${user.tokenID}, Photo: `;
            if (user.photo) {
                const photo = document.createElement('img');
                photo.src = user.photo;
                photo.style.maxWidth = '100px';
                listItem.appendChild(photo);
            }
            userList.appendChild(listItem);
        });
        userListContainer.appendChild(userList);
    })
    .catch(error => {
        alert('Error fetching users: ' + error.message);
    });
});
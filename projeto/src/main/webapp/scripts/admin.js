document.addEventListener('DOMContentLoaded', function() {
    var tokenButton = document.getElementById('tokenButton');

    tokenButton.addEventListener('click', function(event) {
        event.preventDefault();
        var authToken = localStorage.getItem("authToken");
        if ( authToken != null ) {
            alert("Auth Token Data:\n" + authToken);
        } else {
            alert("Auth Token not found.");
        }
    });
});
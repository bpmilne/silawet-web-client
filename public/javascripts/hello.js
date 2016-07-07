window.onload = function() {
    var x = document.getElementById("mainElement");
    var y = x.getElementsByTagName("SPAN");
    var i;
    for (i = 0; i < y.length; i++) {
        var html = urlify(y[i].innerHTML);
        y[i].innerHTML = html;
    }
};

function urlify(text) {
    var urlRegex =/(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
    return text.replace(urlRegex, function(url) {
        return '<a href="' + url + '">' + url + '</a>';
    })
}
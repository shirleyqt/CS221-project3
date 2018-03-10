

function handleSearch(value) {
	window.location.href = "searchresult.html?query=" + value;
	
}

// bind pressing enter key to a handler function
jQuery('#query').keypress(function(event) {
	// keyCode 13 is the enter key
	if (event.keyCode == 13) {
		// pass the value of the input box to the handler function
		handleSearch($('#query').val())
	}
})

// bind pressing the button to a handler function
document.getElementById('search').onclick = function() { 
	handleSearch($('#query').val()) 
};
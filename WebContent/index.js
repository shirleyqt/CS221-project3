

function handleSearch(value) {
	window.location.href = "searchresult.html?query=" + value;
	
}

// bind pressing enter key to a hanlder function
jQuery('#search').keypress(function(event) {
	// keyCode 13 is the enter key
	if (event.keyCode == 13) {
		// pass the value of the input box to the handler function
		handleSearch($('#search').val())
	}
})



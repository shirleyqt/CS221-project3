

// get parameter
// https://stackoverflow.com/questions/19491336/get-url-parameter-jquery-or-how-to-get-query-string-values-in-js
function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
}

function handleSearchResult(resultData) {
		
	var resultListElement = jQuery("#result_list");
	
	for (var i = 0; i < resultData.length; i++) {
		var rowHTML = "";
		rowHTML += "<li><a href='";
		rowHTML += "//" + resultData[i]["url"]
		rowHTML += "'>";
		rowHTML += resultData[i]["docID"]
		rowHTML += "</a></li>"
		rowHTML += "<p>" + resultData[i]["url"] + "</p>";
		resultListElement.append(rowHTML);
		
	}
	
	
}


var query = getUrlParameter("query");

if (query) {
	jQuery.ajax({
		  dataType: "json",
		  method: "GET",
		  url: "search" + "?query=" + query,
		  success: (resultData) => handleSearchResult(resultData)
	});
	
}

//bind pressing the button to a handler function
document.getElementById('return').onclick = function() { 
	window.location.href = "index.html";
};

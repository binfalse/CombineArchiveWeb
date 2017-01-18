<?php
/*
This script is meant to ease the migration of a webCAT instance to another domain by parsing the workspace cookies
and redirecting to the share endpoint with multiple workspaces, causing webCAT to assign the history cookie to that new domain.
Please note, that this method DOES NOT automatically copy the files in the storage directory.
Also it requires to have the context variable ALLOW_SHARING_HISTORY set to true, to enable mentioned /share/history endpoint.
*/

// base location of new webCAT installation
define("WEBCAT_LOCATION", "https://localhost/");
// location to redirect to, with trailing slash and history endpoint path
define("REDIRECT_LOCATION", WEBCAT_LOCATION . "/rest/history/");
// Show information page first and not directly redirect via HTTP header
define("SHOW_INFO_PAGE", false);

// path of the current workspace
define("COOKIE_PATH", "combinearchiveweba");
// workspace history
define("COOKIE_HISTORY", "combinearchivewebhist");
// user vcard
define("COOKIE_USER", "combinearchivewebuser");

// ----------------------------------------------------------------------------

$success = false;

if( isset($_COOKIE[COOKIE_HISTORY]) and $_COOKIE[COOKIe_HISTORY] != "" ) {
    // history cookie is set -> decode it
    $history = base64_decode($_COOKIE[COOKIE_HISTORY]);
    $history = json_decode($history);

    $result = array();
    foreach( $history as $history_entry ) {
        if( isset($history_entry['current']) and $history_entry['current'] == true ) {
            // this entry is the current workspace entry, so put it on the beginning of the result array
            array_unshift( $result, $history_entry );
        }
        else {
            // normal, not current, entry.
            array_push( $result, $history_entry );
        }
    }

    $redirect_url = REDIRECT_LOCATION . implode(",", $result);
    $success = true;
    if( !SHOW_INFO_PAGE ) {
        header("Location", $redirect_url);
        exit();
    }
}
?><!DOCTYPE html>
<html>
    <head>
        <title>webCAT moved</title>
        <style>
        </style>
    </head>

    <body>
        
    </body>
</html>

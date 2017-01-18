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

if( isset($_COOKIE[COOKIE_HISTORY]) and $_COOKIE[COOKIE_HISTORY] != "" ) {
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
<html lang="en-us">
    <head>
        <title>webCAT moved</title>

        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #CCC; color: black; font: 1em normal "Verdana","Arial","Georgia",sans-serif; text-align: center;}
            a { text-decoration: underline; color: #A00; }
            a:hover { text-decoration: none !important; }
            .frame { width: 570px; height: 100%; margin: 0 auto; padding: 2em 5px; text-align: left; background-color: #EEE; border: 1px solid  #AAA; border-style: none solid none solid; }
            .content {  }
            h1 { width: 100%; font 1.5em bold; line-height: 1.05em; margin-bottom: 2em; }
            p { margin-bottom: 3em; padding: 0 5px; text-align: justify; }
            p.button { text-align: center; }
            a.button { display: box; padding: 0.5em 1em; color: #FFF; background-color: #0D0; text-align: center; vertical-align: center; font: 1.3em bold; text-decoration: none; }
            a.button:hover { background-color: #F00 !important; }
        </style>
    </head>

    <body>
        <div class="frame">
            <div class="content">
                <h1>This webCAT instance moved!</h1>
                
                <p class="explain">
                    This <a href="http://sems.uni-rostock.de/cat" target="_blank">webCAT</a> instance move to a new domain. 
                    <?php if( $success ) { ?>
                    To ensure you can still access your pressures workspaces and archives
                    you can migrate the history cookie, which contains all the workspace ids. To do so please click on the button down below.
                    <?php } else { ?>
                    It does not seem, that you do not had any workspaces stored at this place. So you can just click at the button below to
                    access webCAT at the new domain.
                    <?php } ?>
                </p>
                <p class="button">
                    <a class="button" href="<?php echo($redirect_url); ?>"><?php echo($success ? "Migrate Workspaces" : "Go to new Instance"); ?></a>
                </p>
                <?php if($success) { ?>
                <p class="ws-list">
                    Following workspaces were found:
                    <ul>
                        <?php foreach( $result as $entry ) { ?><li><code><?php echo($entry); ?></code></li><?php } ?>
                    </ul>
                </p>
                <?php } ?>
            </div>
        </div>
    </body>
</html>

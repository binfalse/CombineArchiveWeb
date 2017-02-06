<?php
/*
This script is meant to ease the migration of a webCAT instance to another domain by parsing the workspace cookies
and redirecting to the share endpoint with multiple workspaces, causing webCAT to assign the history cookie to that new domain.
Please note, that this method DOES NOT automatically copy the files in the storage directory.
Also it requires to have the context variable ALLOW_SHARING_HISTORY set to true, to enable mentioned /share/history endpoint.
*/

// base location of new webCAT installation
define("WEBCAT_LOCATION", "https://cat.bio.informatik.uni-rostock.de/");
// location to redirect to, with trailing slash and history endpoint path
define("REDIRECT_LOCATION", WEBCAT_LOCATION . "rest/history/");

// path of the current workspace
define("COOKIE_PATH", "combinearchiveweba");
// workspace history
define("COOKIE_HISTORY", "combinearchivewebhist");
// user vcard
define("COOKIE_USER", "combinearchivewebuser");

// ----------------------------------------------------------------------------

$migrate_url = WEBCAT_LOCATION;
$redirect_url = WEBCAT_LOCATION . substr($_SERVER['REQUEST_URI'], 1);

if( isset($_COOKIE[COOKIE_HISTORY]) and $_COOKIE[COOKIE_HISTORY] != "" ) {
    // history cookie is set -> decode it
    $history = base64_decode($_COOKIE[COOKIE_HISTORY]);
    $history = json_decode($history, true);

    $result = array();
    $names = array();
    foreach( $history as $history_entry ) {
        if( isset($history_entry['current']) and $history_entry['current'] == true ) {
            // this entry is the current workspace entry, so put it on the beginning of the result array
            array_unshift( $result, $history_entry["workspaceId"] );
            $names[$history_entry["workspaceId"]] = $history_entry["name"];
        }
        else {
            // normal, not current, entry.
            array_push( $result, $history_entry["workspaceId"] );
            $names[$history_entry["workspaceId"]] = $history_entry["name"];
        }
    }

    $migrate_url = REDIRECT_LOCATION . implode(",", $result);
}
else {
    // redirect directly to new page, using the proper endpoint, since no cookie was set.
    header("Location", $redirect_url);
    exit();
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
            .frame { width: 570px; height: 100%; margin: 0 auto; padding: 0 5px; text-align: left; background-color: #EEE; border: 1px solid  #AAA; border-style: none solid none solid; }
            .content {  }
            div.header { padding-top: 2em !important; }
            div.header > h1 { width: 100%; font 1.5em bold; line-height: 1.05em; margin: 0; }
            p, div.text { margin-bottom: 3em; padding: 0 5px; text-align: justify; }
            iframe { width: 100%; height: 3em; border: 1px solid #AAA }
            div.button { margin-bottom: 3em; padding: 0 5px; text-align: center; }
            a.button { display: box; padding: 0.5em 1em; color: #FFF; text-align: center; vertical-align: center; font: 1.3em bold; text-decoration: none; font-weight: bold; }
            a.ok { background-color: #0D0; }
            a.ok:hover { background-color: #090 !important; }
            a.warn { background-color: #c68523 }
            a.warn:hover { background-color: #915f15 !important; }
        </style>
    </head>

    <body>
        <div class="frame">
            <div class="content">
                <div class="text header">
                    <h1>This webCAT instance moved!</h1>
                </div>
                <p class="explain">
                    WebCAT is now available from <a href="<?php echo(WEBCAT_LOCATION); ?>" target="_blank"><?php echo(parse_url(WEBCAT_LOCATION, PHP_URL_HOST)); ?></a>.
                    To ensure that you can still access your precious workspaces and archives, we now try to migrate your workspace history.
                </p>

                <div class="button">
                    <a class="button ok" href="<?php echo($redirect_url); ?>">Go to new Instance</a>
                </div>

                <div class="text process">
                    We now attempt to automatically migrate your history... <br />
                    <iframe src="<?php echo($migrate_url); ?>"></iframe>
                    <br />
                    If the frame above does <b>not</b> show a success message, please try to migrate your workspace history manually by clicking on the button below. This might be necessary, if your browser does not allow iframes.
                </div>

                <div class="button">
                    <a class="button warn" href="<?php echo($migrate_url); ?>">Migrate Workspaces Manually</a>
                </div>

                <div class="text ws-list">
                    Following workspaces were found and are ready to be migrated:
                    <ul>
                        <?php foreach( $result as $entry ) { ?>
                        <li>
                            <b><?php echo($names[$entry]); ?></b><br />
                            <code><?php echo($entry); ?></code>
                        </li>
                        <?php } ?>
                    </ul>
                </div>
            </div>
        </div>
    </body>
</html>

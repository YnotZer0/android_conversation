#!/bin/sh

# based on https://odoepner.wordpress.com/2012/02/17/shell-script-to-generate-simple-index-html/

echo '<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>IBM Watson Developer Cloud</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>IBM Watson Developer Cloud Android SDK</h1>
    </div>

    <p><a href="http://www.ibm.com/watson/developercloud/">Info</a>
        | <a href="http://www.ibm.com/watson/developercloud/doc/">Documentation</a>
        | <a href="https://github.com/watson-developer-cloud/android-sdk">GitHub</a>
    </p>

    <p>Javadoc by branch/tag:</p>
    <ul>'
ls docs | grep --invert-match index.html | sed 's/^.*/<li><a href="docs\/&">&<\/a><\/li>/'
echo '    </ul>
</div>
</body>
</html>'

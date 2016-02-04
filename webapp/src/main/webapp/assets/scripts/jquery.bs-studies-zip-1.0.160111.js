/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

(function($, undefined) {
    if($ == undefined) throw "jQuery not loaded";
    $(function() {
        var filename = $('#filename').val();
        var dc = $('#dc').val();
        var accession = $('#accession').val();
        setTimeout(checkStatus,2000);

        function checkStatus(){
            $.get( contextPath+"/"+dc+"/zipstatus", { filename: filename}, function(data) {
                if(data) {
                    switch (data.status) {
                        case 'processing':
                            setTimeout(checkStatus,2000);
                            break;
                        case 'done':
                            link = contextPath+"/"+dc+"/files/"+accession+"/zip?file="+filename;
                            $('#ftp-link').html('<a href="'+link+'">Click here to download the file</a>')
                            break;
                        default:
                            err();
                            break;
                    }
                } else {
                    err();
                }
            })
            .fail(function() {
                err();
            })
        }

        function err() {
            $('#ftp-link').html('An error occured while preparing the archive. Please try again later.<br/> If the problem persists, please use the feedback form to report it.');
        }

     });
})(window.jQuery);

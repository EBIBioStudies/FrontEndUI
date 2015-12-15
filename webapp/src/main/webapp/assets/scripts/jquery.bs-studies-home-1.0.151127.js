/*
 * Copyright 2009-2015 European Molecular Biology Laboratory
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
        $.getJSON( contextPath + "/servlets/query/-/home/json?type=Study&fo=1&pagesize=5&sortby=release_date", function( data ) {
            $('#studyCount').text(data.studies.total + (data.studies.total==1 ? ' study' : ' studies') );
            if (data.studies.study) {
                $.each(data.studies.study.slice(0, 5), function (i, v) {
                    $('#latestList').append('<li><a href="studies/' + v.accession + '">' + v.title
                        + '</a><span class="browse-study-accession">' + v.accession + '</span></li>')
                });
                $('#studyCountStats').fadeIn();
                $('#latest').fadeIn();
            }
        });
        $.getJSON( contextPath + "/servlets/query/-/home/json?type=Project", function( data ) {
            if (data.studies.total>0) {
                $('#projectCount').text(data.studies.total + (data.studies.total == 1 ? ' project' : ' projects'));
                $('#projectCountStats').fadeIn();
            }
        });
     });
})(window.jQuery);

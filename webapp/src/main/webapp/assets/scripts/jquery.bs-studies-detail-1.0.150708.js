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
    if($ == undefined)
        throw "jQuery not loaded";

    var isExpanded = false;
    $(function() {

        $('#filter-file-name').keyup(function(){
            var query = $(this).val().toLowerCase();
            $('ul#file-list > li > a').each(function(){
                var text = $(this).text().toLowerCase();
                (text.indexOf(query) >= 0) ? $(this).parent().show() : $(this).parent().hide();
            });
            addShowMore();
        });
        addShowMore();
    });

    function addShowMore() {
        $("ul#file-list").readmore(
            {
                moreLink: '<a href="#" class="show-more">show all '+  $('ul#file-list > li > a:visible').size()+' </a>',
                lessLink: '<a href="#" class="show-less">show less</a>',
                embedCSS: false,
                startOpen: isExpanded,
                afterToggle: function(trigger, element, expanded) {
                    if(!expanded) { // The "Close" link was clicked
                        $('#filter-file-name').focus();
                    }
                    isExpanded = expanded;
                }
            }
        );

    }

})(window.jQuery);

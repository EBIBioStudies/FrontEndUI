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
            $('ul#file-list > li').each(function(){
                var text = $(this).text().toLowerCase();
                (text.indexOf(query) >= 0) ? $(this).show() : $(this).hide();
                // expand attributes if there's a match in them
                if (text.replace($('a:first',this).text().toLowerCase(),'').indexOf(query) > 0) {
                    showFileAttributes($('.file-attribute-expander',this));
                }
            });
            addShowMore();
        });

        $('.file-attribute-expander').click(function() {
            var tbl = $('#'+$(this).data('attribute-table'));
            if (tbl.css('display')=='none')
                showFileAttributes(this);
            else
                hideFileAttributes(this);
            addShowMore();
        });

        // call after page load
        addShowMore();

    });

    function showFileAttributes(obj){
        $('#'+$(obj).data('attribute-table')).show();
        $(obj).removeClass('file-attribute-expander-off').addClass('file-attribute-expander-on');
    }


    function hideFileAttributes(obj){
        $('#'+$(obj).data('attribute-table')).hide();
        $(obj).removeClass('file-attribute-expander-on').addClass('file-attribute-expander-off');
    }

    function addShowMore() {
        var allShown = $('ul#file-list > li:visible').size() == $('ul#file-list > li').size();
        $("ul#file-list").readmore(            {
                moreLink: ('<a href="#" class="show-more">show '+ ( allShown ? 'all '  : 'filtered ')+ $('ul#file-list > li:visible').size() +' </a>'),
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

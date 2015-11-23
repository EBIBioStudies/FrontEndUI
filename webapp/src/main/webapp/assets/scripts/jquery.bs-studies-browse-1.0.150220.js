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
        // set default sorting order if none is specified. Changes should be reflected in Querier.java too.
        $("option.sortable").aeBrowseSorting({
             defaultField: "relevance"
             , fields: {
                 relevance: {title: "relevance", sort: "descending"}
                 , accession: {title: "accession", sort: "ascending"}
                 , title: {title: "title", sort: "ascending"}
                 , authors: {title: "authors", sort: "ascending"}
                 , release_date: {title: "release date", sort: "descending"}
                 , files: {title: "number of files", sort: "descending"}
                 , links: {title: "number of links", sort: "descending"}
             }
         });
         $("#studies-browse-sorter").bind('change', function () {
             window.location = $("#studies-browse-sorter").find(":selected").attr("data-url");
         });

        $(".search-authors").each( function() {
            var showSize = 10;
            $(this).data('authors',$(this).html());
            var values = $(this).html().split(',');
            $(this).html('<span class="authors">'+values.slice(0,showSize).join(',')+'</span>');
            if (values.length <= showSize) return;
            $(this).append(
                $('<span>, <a class="show-more">show ' + (values.length - showSize) +' more</a></span>')
                .click( function() {
                        $(this).hide();
                        $(this).parent().find('.authors').html(($(this).parent().data('authors')));
                        $('a.show-less',$(this).parent()).parent().show();
                })
            ).append( $('<span> <a class="show-less">show less</a></span>')
                    .hide().click( function () {
                        $(this).hide();
                        $(this).parent().find('.authors').html(values.slice(0,showSize).join(','));
                        $('a.show-more',$(this).parent()).parent().show();
                }))



            /*$('ul.authors li:nth-child(n+4)', $(this)).hide();
            $('ul.authors', $(this)).append(
                $('<a class="show-more">show ' + $('ul.authors li:hidden').length +' more</a>').click(function () {
                    $(this).parent().find('li').show();
                    $(this).hide();
                    $(this).parent().find('.show-less').show();
                })
            ).append(
                $('<a class="show-less">show less</a>').click(function () {
                    $(this).parent().find('.show-more').show();
                    $(this).hide();
                    $('ul.authors li', $(this).parent('ul')).show();
                }).hide()
            );*/

        });
     });
})(window.jQuery);

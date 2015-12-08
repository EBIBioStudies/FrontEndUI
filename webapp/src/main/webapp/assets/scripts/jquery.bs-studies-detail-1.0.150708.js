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
var filesTable = null;
var linksTable = null;

(function($, undefined) {
    if($ == undefined)
        throw "jQuery not loaded";

    var selectedFilesCount = 0;
    var totalRows = $("#file-list tbody tr").length;

    $(function() {
        //turn off all selected files
        $('input:checkbox:not(.do-not-clear)').prop('checked', false);

        // capture hover before datatable is rendered
        $(".file-link").hover(
            function () {showThumbnail($(this));},
            function () {hideThumbnail($(this));}
        );
        $(".file-link").prev().hover(
            function () {showThumbnail($(this).next());},
            function () {hideThumbnail($(this).next());}
        );

        // create all sub-section file tables and hide them
        $(".file-list:not(#file-list)").DataTable( {
            "dom":"t",
            "scrollX" : "100%"
        });
        $(".ae-section-files").hide();
        $(".link-list:not(.link-widget)").DataTable( {
            "dom":"t",
            "scrollX" : "100%"
        });
        $(".ae-section-links").hide();


        // handle file selection
        /*$("#file-list tbody").on( 'click', 'tr', function () {
            $(this).toggleClass('selected');
            if ( $(this).hasClass('selected')) {
                $('input[type="checkbox"]',$(this)).attr('checked','checked');
            } else {
                $('input[type="checkbox"]',$(this)).removeAttr('checked');
            }
            updateSelectedFiles();
        });*/


        $("#file-list tbody").on( 'click', 'input[type="checkbox"]', function () {
            $(this).toggleClass('selected');
            updateSelectedFiles($(this).hasClass('selected') ? 1: -1);
        });

        $("#file-list tbody tr td a").on( 'click', function () {
            event.stopPropagation();
        });

        $("#download-selected-files").on( 'click', function () {
            // select all checked input boxes and get the href in the links contained in their siblings
            var files = $.map($('a',$('input[checked]', filesTable.cells().nodes()).parent().next()), function (v) {
                return $(v).data('name');
            });
            downloadFiles(files);
        });

        $("#select-all-files").on ('click', function () {
            var isChecked = $(this).is(':checked');
            if (!isChecked) {
                $('input[type="checkbox"]', filesTable.cells().nodes()).removeAttr('checked');
                $('input[type="checkbox"]', filesTable.cells().nodes()).parent().parent().removeClass('selected');
                selectedFilesCount = 0;
            } else {
                $('input[type="checkbox"]', filesTable.cells().nodes()).attr('checked', 'checked');
                $('input[type="checkbox"]', filesTable.cells().nodes()).parent().parent().addClass('selected');
                selectedFilesCount = totalRows;
            }
            updateSelectedFiles(0);
        });

        $(window).resize(function () {
            redrawTables();
        });

        $(".toggle-files").on ('click', function () {
            var section = $(this).first().next();
            if (section.css('display')=='none') {
                section.show();
                $(this).text('hide files in this section')
            } else {
                section.hide();
                $(this).text('show files in this section')
            }

        });

        $(".toggle-links").on ('click', function () {
            var section = $(this).first().next();
            if (section.css('display')=='none') {
                section.show();
                $(this).text('hide links in this section')
            } else {
                section.hide();
                $(this).text('show links in this section')
            }

        });

        // draw the main file table
        redrawTables();
        redrawTables(); // needed to adjust the column width. TODO: Find a better solution
        updateSelectedFiles(0);

        // draw subsection and hide them
        $(".indented-section").parent().prev().prepend('<span class="toggle-section fa fa-expand fa-icon" title="Click to expand"/>')
        $(".indented-section").hide();

        $('.toggle-section').parent().css('cursor','pointer');
        $('.toggle-section').parent().on('click', function() {
            var indented_section = $(this).next().children().first();
            if ( indented_section.css('display') == 'none') {
                $(this).children().first().toggleClass('fa-compress').toggleClass('fa-expand');
                indented_section.show();
            } else {
                $(this).children().first().toggleClass('fa-compress').toggleClass('fa-expand');
                indented_section.hide();
            }
        })

        // add link type filters
        $(".link-filter").on('change', function() {
            var filters = $(".link-filter:checked").map(function() { return '^'+this.id+'$'}).get();
            if (filters.length==0) {
                /*$(this).attr('checked','checked');                return;*/
                filters = ['^$']
            }
            linksTable[$(this).data('position')-1].column(1).search(filters.join('|'),true, false).draw()
        });

    });

    function showThumbnail(fileLink) {
        $(".thumbnail-image", $(fileLink)).data('isFocused', true);
        if (! $(fileLink).data('thumbnail')) return;
        if (!$(fileLink).data("loaded")) {
            $(".thumbnail-image", $(fileLink)).attr("src",($(fileLink).data('thumbnail')));
            $(".thumbnail-loader", $(fileLink)).show();
        } else {
            $(".thumbnail-image", $(fileLink)).css('visibility','visible');
            $(".thumbnail-image", $(fileLink)).stop().animate({opacity: 1});
        }
        $(".thumbnail-image", $(fileLink)).one("load",function(){
            $(this).css({"position":"absolute", "max-width":"150px"})
            if($(this).data("isFocused")) {
                $(this).css('visibility','visible');
                $(this).stop().animate({opacity: 1});
            }
            $(".thumbnail-loader",$(this).parent()).css('visibility','hidden');
            $(this).parent().parent().data("loaded",true);
        });
    }

    function hideThumbnail(fileLink) {
        $(".thumbnail-image", $(fileLink)).stop().animate({opacity: 0});
        $(".thumbnail-image", $(fileLink)).css('visibility','hidden');
        $(".thumbnail-loader", $(fileLink)).css('visibility','hidden');
        $(".thumbnail-image", $(fileLink)).data('isFocused', false);
    }



    function downloadFiles(files) {
        var html = '';
        if (files.length==1) {
            html += '<form method="GET" target="_blank" action="' + ($('#project').length>0 ? '../' :'') + "../../files/" + $('.accessionNumber').text() + '/' + files[0]+'" />';
        } else {
            html += '<form method="POST" target="_blank" action="'+ ($('#project').length>0 ? '../' :'') + "../../files/" + $('.accessionNumber').text() + '/zip'+location.search+'">';
            $(files).each( function(i,v) {
                html += '<input type="hidden" name="files" value="'+v+'"/>'
            });
            html += '</form>';
        }
        var submissionForm = $(html);
        $('body').append(submissionForm);
        $(submissionForm).submit();
    }

    function updateSelectedFiles(inc)
    {

        if (!filesTable || !filesTable.rows() || !filesTable.rows().eq(0) ) return;
        selectedFilesCount += inc;
        $("#selected-file-text").text( (selectedFilesCount == 0
                ? 'No ' : selectedFilesCount)
                +' file'+(selectedFilesCount>1 ? 's':'')+' selected');
        if (selectedFilesCount==0) {
            $('#download-selected-files').hide();
        } else {
            $('#download-selected-files').show();
            $('#download-selected-files').text('Download' + (selectedFilesCount==2
                    ? ' both'
                    : selectedFilesCount>1 ? ' all '+selectedFilesCount : ''));
        }

        if (selectedFilesCount==totalRows)
            $("#select-all-files").attr('checked', 'checked');
        else
            $("#select-all-files").removeAttr('checked');

    }

    function redrawTables() {
        if (filesTable == null) {
            filesTable = $("#file-list").DataTable({
                "lengthMenu": [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
                "columnDefs": [{"targets": [0], "searchable": false, "orderable": false, "visible": true}],
                "order": [[1, "asc"]],
                "dom": "rlftpi",
                "scrollX" : "100%"
            });
        } else {
            filesTable.columns.adjust().draw();
        }

        if (linksTable == null) {
            linksTable = [];
            $(".link-widget").each(function () {
                linksTable.push($(this).DataTable({
                        "lengthMenu": [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
                        "dom": "rlftpi",
                        "scrollX" : "100%"
                    })
                );
            });
        } else {
            $(linksTable).each(function() {
                this.columns.adjust().draw();
            });
        }

        $(".list-content").show()
        $(".list-loader").hide();
    }

    $('.org-link').click( function() {
        var href = $(this).attr('href');
        if (!$(href).is(':visible')) {
            $('#hidden-orgs').find('a.show-more').click()
        }

        $('html, body').animate({
            scrollTop: $(href).offset().top -10
        }, 200);

        $(href).next().next().animate({opacity:0.8}, 200, function(){
            $(href).next().next().css('background-color','yellow');
                $(href).next().next().animate({opacity:0.4}, 400, function(){
                    $(href).next().next().css('background-color','lightgray');
                    $(href).next().next().animate({opacity:1},600);
                    $(href).next().next().css('background-color','transparent');
            })
        });

    });

    $('#right-column-expander').click( function() {
        $('#ae-detail-right-column').toggleClass('expanded-right-column');
        $(this).toggleClass('fa-compress').toggleClass('fa-expand');
        $(this).attr('title', $(this).hasClass('fa-expand') ? 'Click to expand' : 'Click to collapse')
        $("table.link-widget tbody td a").css("max-width", $(this).attr('data-icon')=='u' ? '200px' : '600px')
        redrawTables()
    });

})(window.jQuery);


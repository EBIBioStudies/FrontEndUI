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

    $(function() {
        //turn off all selected files
        $('input:checkbox').prop('checked', false);

        //setup thumbnails
        $(".file-link").append("<div class='thumbnail-div'><img class='thumbnail-loader' src='../../assets/images/ajax-loader.gif'/><img class='thumbnail-image' /></div>");
        // capture hover before datatable is rendered
        $(".file-link").on('mouseenter',function() { showThumbnail($(this)); });
        $(".file-link").on('mouseleave', function() { hideThumbnail($(this)); });
        $(".file-link").prev().on('mouseenter',function() { showThumbnail($(this).next()); });
        $(".file-link").prev().on('mouseleave', function() { hideThumbnail($(this).next()); });

        // create all sub-section file tables and hide them
        $(".file-list:not(#file-list)").DataTable( {
            "scrollX": true,
            "dom":"t"
        });
        $(".ae-section-files").hide();

        // handle file selection
        $("#file-list tbody").on( 'click', 'tr', function () {
            $(this).toggleClass('selected');
            if ( $(this).hasClass('selected')) {
                $('input[type="checkbox"]',$(this)).attr('checked','checked');
            } else {
                $('input[type="checkbox"]',$(this)).removeAttr('checked');
            }
            updateSelectedFiles();
        });


        $("#file-list tbody").on( 'click', 'input[type="checkbox"]', function () {
            $(this).toggleClass('selected');
            updateSelectedFiles();
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
            } else {
                $('input[type="checkbox"]', filesTable.cells().nodes()).attr('checked', 'checked');
                $('input[type="checkbox"]', filesTable.cells().nodes()).parent().parent().addClass('selected');
            }
            updateSelectedFiles();
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

        // draw the main file table
        redrawTables();
        updateSelectedFiles();
    });

    function showThumbnail(fileLink) {
        $(".thumbnail-image", $(fileLink)).data('isFocused', true);
        if (! $(fileLink).data('thumbnail')) return;
        if (!$(fileLink).data("loaded")) {
            $(".thumbnail-image", $(fileLink)).attr("src",($(fileLink).data('thumbnail')));
            $(".thumbnail-loader", $(fileLink)).show();
        } else {
            $(".thumbnail-image", $(fileLink)).fadeIn("fast");
        }
        $(".thumbnail-image", $(fileLink)).one("load",function(){
            $(this).css({"position":"absolute", "max-width":"150px"})
            if($(this).data("isFocused")) {
                $(this).fadeIn("fast");
            }
            $(".thumbnail-loader",$(this).parent()).hide();
            $(this).parent().parent().data("loaded",true);
        });
    }

    function hideThumbnail(fileLink) {
        $(".thumbnail-image", $(fileLink)).hide();
        $(".thumbnail-loader", $(fileLink)).hide();
        $(".thumbnail-image", $(fileLink)).data('isFocused', false);
    }



    function downloadFiles(files) {
        var html = '';
        if (files.length==1) {
            html += '<form method="GET" action="' + "../../files/" + $('.accessionNumber').text() + '/' + files[0]+'" />';
        } else {
            html += '<form method="POST" action="' + "../../files/" + $('.accessionNumber').text() + '/zip">';
            $(files).each( function(i,v) {
                html += '<input type="hidden" name="files" value="'+v+'"/>'
            });
            html += '</form>';
        }

        var submissionForm = $(html);
        $('body').append(submissionForm);
        $(submissionForm).submit();
    }

    function updateSelectedFiles()
    {
        if (!filesTable || !filesTable.rows() || !filesTable.rows().eq(0) ) return;
        var totalRows = filesTable.rows().eq(0).length;
        var selectedRows = $('input:checked', filesTable.cells().nodes()).length;
        $("#selected-file-text").text( (selectedRows == 0 ? 'No ' : selectedRows) +' file'+(selectedRows>1 ? 's':'')+' selected');
        if (selectedRows==0) {
            $('#download-selected-files').hide();
        } else {
            $('#download-selected-files').show();
            $('#download-selected-files').text('Download' + (selectedRows==2 ? ' both' : selectedRows>1 ? ' all '+selectedRows : ''));
        }

        if (selectedRows==totalRows)
            $("#select-all-files").attr('checked', 'checked');
        else
            $("#select-all-files").removeAttr('checked');

    }

    function redrawTables() {
        if(filesTable!=null) filesTable.destroy()
        filesTable = $("#file-list").DataTable( {
            "lengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]],
            "scrollX": true,
            "columnDefs": [  { "targets": [0], "searchable": false, "orderable": false, "visible": true}],
            "order": [[ 1, "asc" ]],
            "dom":"lfrtpi",
            "autoWidth" : false
        } );

        if(linksTable!=null) linksTable.destroy()
        linksTable = $("#links-table").DataTable( {
            "lengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]],
            "scrollX": true,
            "order": [[ 0, "asc" ]],
            "dom":"lfrtpi",
            "autoWidth" : false
        } );
    }

    $('.org-link').click( function() {
        var href = $(this).attr('href');
        if (!$(href).is(':visible')) {
            $('#hidden-orgs').find('a.show-more').click()
        }

        $('html, body').animate({
            scrollTop: $(href).offset().top
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
        $(this).attr('data-icon', $(this).attr('data-icon')=='u' ? 'w': 'u' );
        $(this).attr('title', $(this).attr('data-icon')=='u' ? 'Click to expand' : 'Click to collapse')
        redrawTables();
    });

})(window.jQuery);


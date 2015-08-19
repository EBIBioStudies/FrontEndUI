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
var table = null;

(function($, undefined) {
    if($ == undefined)
        throw "jQuery not loaded";

    $(function() {

        // create all sub-section file tables and hide them
        $(".file-list:not(#file-list)").DataTable( {
            "scrollX": true,
            "dom":"t",
            "autoWidth":false
        });
        $(".ae-section-files").hide();

        // draw the main file table
        redrawTable();
        updateSelectedFiles();

        $("#file-list tbody").on( 'click', 'tr', function () {
            $(this).toggleClass('selected');
            if ( $(this).hasClass('selected')) {
                $('input[type="checkbox"]',$(this)).attr('checked','checked');
            } else {
                $('input[type="checkbox"]',$(this)).removeAttr('checked');
            }
            updateSelectedFiles();
        });

        $("#file-list tbody tr").on( 'click', 'a', function () {
            event.stopPropagation();
        });

        $("#file-list tbody").on( 'click', 'input[type="checkbox"]', function () {
            $(this).toggleClass('selected');
            updateSelectedFiles();
        });

        $("#download-selected-files").on( 'click', function () {
            // select all checked input boxes and get the href in the links contained in their siblings
            var files = $.map($('a',$('input[checked]', table.cells().nodes()).parent().next()), function (v) {
                return $(v).attr('href');
            });
            downloadFiles(files);
        });

        $("#select-all-files").on ('click', function () {
            var isChecked = $(this).is(':checked');
            if (!isChecked) {
                $('input[type="checkbox"]', table.cells().nodes()).removeAttr('checked');
                $('input[type="checkbox"]', table.cells().nodes()).parent().parent().removeClass('selected');
            } else {
                $('input[type="checkbox"]', table.cells().nodes()).attr('checked', 'checked');
                $('input[type="checkbox"]', table.cells().nodes()).parent().parent().addClass('selected');
            }
            updateSelectedFiles();
        });

        $(window).resize(function () {
            redrawTable();
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
    });

    function downloadFiles(files) {
        $(files).each( function(i,v) {
            var ifr=$('<iframe/>', {
                id:'MainPopupIframe',
                src:v,
                style:'display:none'
            });
            $('body').append(ifr);
         });
    }

    function updateSelectedFiles()
    {
        if (!table || !table.rows() || !table.rows().eq(0) ) return;
        var totalRows = table.rows().eq(0).length;
        var selectedRows = $('input:checked', table.cells().nodes()).length;
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

    function redrawTable() {
        if(table!=null) table.destroy()
        table = $("#file-list").DataTable( {
            "lengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]],
            "scrollX": true,
            "columnDefs": [  { "targets": [0], "searchable": false, "orderable": false, "visible": true}],
            "order": [[ 1, "asc" ]],
            "dom":"lfrtpi",
            "autoWidth" : false
        } );
    }
    $('#right-column-expander').click( function() {
        $('#ae-detail-right-column').toggleClass('expanded-right-column');
        $(this).attr('data-icon', $(this).attr('data-icon')=='u' ? 'w': 'u' );
        $(this).attr('title', $(this).attr('data-icon')=='u' ? 'Click to expand' : 'Click to collapse')
        redrawTable();
    });

})(window.jQuery);

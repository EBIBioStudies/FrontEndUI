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
var table = undefined;

(function($, undefined) {
    if($ == undefined)
        throw "jQuery not loaded";

    $(function() {
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

        $("#file-list tbody").on( 'click', 'input[type="checkbox"]', function () {
            $(this).toggleClass('selected');
            updateSelectedFiles();
        });

        $("#select-all-files").on ('click', function () {
            var isChecked = $(this).is(':checked');
            if (!isChecked) {
                $('input[type="checkbox"]', table.cells().nodes()).removeAttr('checked', isChecked);
                $('input[type="checkbox"]', table.cells().nodes()).parent().parent().removeClass('selected');
            } else {
                $('input[type="checkbox"]', table.cells().nodes()).prop('checked', 'checked');
                $('input[type="checkbox"]', table.cells().nodes()).parent().parent().addClass('selected');
            }
            updateSelectedFiles();
        });
    });

    function updateSelectedFiles()
    {
        var totalRows = table.rows().eq(0).length;
        var selectedRows = $('input:checked', table.cells().nodes()).length;
        $("#selected-file-text").text( (selectedRows == 0 ? 'No ' : selectedRows) +' file'+(selectedRows>1 ? 's':'')+' selected');
        if (selectedRows==totalRows)
            $("#select-all-files").attr('checked', 'checked');
        else
            $("#select-all-files").removeAttr('checked');

    }

    function redrawTable() {
        if(table) table.destroy()
        table = $("#file-list").DataTable( {
            "lengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]],
            "scrollX": true,
            "columnDefs": [  { "targets": [0], "searchable": false, "orderable": false, "visible": true}],
            "order": [[ 1, "asc" ]],
            "dom":"lfrtpi"
        } );
    }
    $('#right-column-expander').click( function() {
        $('#ae-detail-right-column').toggleClass('expanded-right-column');
        $(this).attr('data-icon', $(this).attr('data-icon')=='u' ? 'w': 'u' );
        redrawTable();
    });

})(window.jQuery);

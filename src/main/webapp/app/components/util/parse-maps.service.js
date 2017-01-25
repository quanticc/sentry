(function () {
    'use strict';

    angular
        .module('sentryApp')
        .factory('ParseMaps', ParseMaps);

    function ParseMaps() {

        let service = {
            parseToEntries: parseToEntries,
            parseToMap: parseToMap
        };

        return service;

        function parseToEntries(json) {
            let result = [];
            let map = flatten(json);
            for (let key in map) {
                if (map.hasOwnProperty(key)) {
                    let data = {};
                    data.name = key;
                    data.value = map[key];
                    result.push(data);
                }
            }
            return result;
        }

        function parseToMap(entries) {
            var data = {};
            for (let entry in entries) {
                if (entries.hasOwnProperty(entry)) {
                    let key = entries[entry].name;
                    data[key] = entries[entry].value;
                }
            }
            return unflatten(data);
        }

        function unflatten(data) {
            if (Object(data) !== data || Array.isArray(data))
                return data;
            var regex = /\.?([^.\[\]]+)|\[(\d+)\]/g,
                resultholder = {};
            for (var p in data) {
                var cur = resultholder,
                    prop = "",
                    m;
                while (m = regex.exec(p)) {
                    cur = cur[prop] || (cur[prop] = (m[2] ? [] : {}));
                    prop = m[2] || m[1];
                }
                cur[prop] = data[p];
            }
            return resultholder[""] || resultholder;
        }

        function flatten(data) {
            var result = {};

            function recurse(cur, prop) {
                if (Object(cur) !== cur) {
                    result[prop] = cur;
                } else if (Array.isArray(cur)) {
                    for (var i = 0, l = cur.length; i < l; i++)
                        recurse(cur[i], prop + "[" + i + "]");
                    if (l == 0)
                        result[prop] = [];
                } else {
                    var isEmpty = true;
                    for (var p in cur) {
                        isEmpty = false;
                        recurse(cur[p], prop ? prop + "." + p : p);
                    }
                    if (isEmpty && prop)
                        result[prop] = {};
                }
            }

            recurse(data, "");
            return result;
        }
    }
})();

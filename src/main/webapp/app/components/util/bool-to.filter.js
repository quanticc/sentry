(function() {
    'use strict';

    angular
        .module('sentryApp')
        .filter('boolTo', boolTo);

    function boolTo() {
        return boolToFilter;

        function boolToFilter (input, trueValue, falseValue) {
            if (input === true) {
                return trueValue;
            } else {
                return falseValue;
            }
        }
    }
})();

(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('BotDetailController', BotDetailController);

    BotDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Bot'];

    function BotDetailController($scope, $rootScope, $stateParams, previousState, entity, Bot) {
        var vm = this;

        vm.bot = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:botUpdate', function(event, result) {
            vm.bot = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();

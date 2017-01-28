(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PlayerCountDetailController', PlayerCountDetailController);

    PlayerCountDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'PlayerCount'];

    function PlayerCountDetailController($scope, $rootScope, $stateParams, previousState, entity, PlayerCount) {
        var vm = this;

        vm.playerCount = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:playerCountUpdate', function(event, result) {
            vm.playerCount = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();

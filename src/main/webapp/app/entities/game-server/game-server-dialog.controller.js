(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('GameServerDialogController', GameServerDialogController);

    GameServerDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'GameServer'];

    function GameServerDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, GameServer) {
        var vm = this;

        vm.gameServer = entity;
        vm.clear = clear;
        vm.datePickerOpenStatus = {};
        vm.openCalendar = openCalendar;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.gameServer.id !== null) {
                GameServer.update(vm.gameServer, onSaveSuccess, onSaveError);
            } else {
                GameServer.save(vm.gameServer, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:gameServerUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }

        vm.datePickerOpenStatus.expirationDate = false;
        vm.datePickerOpenStatus.expirationCheckDate = false;
        vm.datePickerOpenStatus.statusCheckDate = false;
        vm.datePickerOpenStatus.lastValidPing = false;
        vm.datePickerOpenStatus.lastRconDate = false;
        vm.datePickerOpenStatus.lastGameUpdate = false;
        vm.datePickerOpenStatus.lastUpdateStart = false;
        vm.datePickerOpenStatus.lastRconAnnounce = false;

        function openCalendar (date) {
            vm.datePickerOpenStatus[date] = true;
        }
    }
})();

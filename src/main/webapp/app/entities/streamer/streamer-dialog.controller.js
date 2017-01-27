(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('StreamerDialogController', StreamerDialogController);

    StreamerDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Streamer', 'ParseMaps'];

    function StreamerDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Streamer, ParseMaps) {
        var vm = this;

        vm.providerTypes = ['Twitch'];
        vm.leagueTypes = ['TF2', 'OW', '*'];
        vm.divisionTypes = [
            'HL',
            'HL NA',
            'HL NA Platinum',
            'HL NA Silver',
            'HL NA Steel',
            '6s',
            '6s NA',
            '6s NA Platinum',
            '6s NA Silver',
            '6s NA Steel',
            'HL/6s NA Platinum',
            'HL/4s NA Silver',
            'HL NA Platinum, 6s NA Silver',
            'HL NA Silver, 6s NA Steel',
            'HL/4s NA Silver, 6s NA Steel'
        ];
        vm.announcementTypes = [
            '@here {{name}} ({{league}} {{division}}) is now live on <{{url}}>!',
            '@here {{name}} is now live on <{{url}}>!',
            '@here [**MATCH CAST**] {{name}} is now live on <{{url}}>!'
        ];
        vm.streamer = entity;
        vm.entries = ParseMaps.parseToEntries(vm.streamer.embedFields);
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
            vm.streamer.embedFields = ParseMaps.parseToMap(vm.entries);
            if (vm.streamer.id !== null) {
                Streamer.update(vm.streamer, onSaveSuccess, onSaveError);
            } else {
                Streamer.save(vm.streamer, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:streamerUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }

        vm.datePickerOpenStatus.lastAnnouncement = false;

        function openCalendar (date) {
            vm.datePickerOpenStatus[date] = true;
        }
    }
})();

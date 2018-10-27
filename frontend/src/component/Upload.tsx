import { Button, Checkbox, FormControlLabel, TextField } from '@material-ui/core';
import * as React from 'react';
import Dropzone from 'react-dropzone';

interface IUploadState {
    files: File[]
}

export default class Upload extends React.Component<{}, IUploadState> {
    constructor(props: any) {
        super(props);
        this.state = { files: [] }
        this.onDrop = this.onDrop.bind(this)
    }

    public onDrop(files: File[]) {
        window.console.log("dropped something");
        this.setState({
            files
        });
    }

    public render() {
        return (
            <section>
                <div className="dropzone">
                    <Dropzone onDrop={this.onDrop} >
                        <p>Try dropping some files here, or click to select files to upload.</p>
                    </Dropzone>
                    <TextField title="name" label="name" />
                    <TextField title="tag" label="tag" />
                    <TextField title="parent" label="parent" />
                    <br />
                    <Button variant="contained" color="primary">Upload</Button>
                    <FormControlLabel
                        control={
                            <Checkbox />
                        }
                        label="through server"
                    />
                </div>
                <aside>
                    <h2>Dropped files</h2>
                    <ul>
                        {
                            this.state.files.map(f => <li key={f.name}>{f.name} - {f.size} bytes</li>)
                        }
                    </ul>
                </aside>
            </section>
        );
    }
}